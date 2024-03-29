/*
 * Copyright (c) 2003 Frank Sauer. All rights reserved.
 *
 * Licenced under CPL 1.0 (Common Public License Version 1.0).
 * The licence is available at http://www.eclipse.org/legal/cpl-v10.html.
 *
 *
 * DISCLAIMER OF WARRANTIES AND LIABILITY:
 *
 * THE SOFTWARE IS PROVIDED "AS IS".  THE AUTHOR MAKES  NO REPRESENTATIONS OR WARRANTIES,
 * EITHER EXPRESS OR IMPLIED.  TO THE EXTENT NOT PROHIBITED BY LAW, IN NO EVENT WILL THE
 * AUTHOR  BE LIABLE FOR ANY DAMAGES, INCLUDING WITHOUT LIMITATION, LOST REVENUE,  PROFITS
 * OR DATA, OR FOR SPECIAL, INDIRECT, CONSEQUENTIAL, INCIDENTAL  OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF  LIABILITY, ARISING OUT OF OR RELATED TO
 * ANY FURNISHING, PRACTICING, MODIFYING OR ANY USE OF THE SOFTWARE, EVEN IF THE AUTHOR
 * HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 *
 * $id$
 */
package net.sourceforge.metrics.core;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import net.sourceforge.metrics.core.sources.Cache;
import net.sourceforge.metrics.propagators.Propagator;
import net.sourceforge.metrics.propagators.Sum;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class providing access to the metrics framework.
 * 
 * @author Frank Sauer
 */
public class MetricsPlugin extends AbstractUIPlugin implements Preferences.IPropertyChangeListener, Constants {

	
	private HashMap metricsDependencies;
	private String[] descriptions;
	private String[] ids;
	//The shared instance.
	private static MetricsPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	private boolean listenerAdded = false;
	
	private HashMap calculators = new HashMap();
	private HashMap metrics = new HashMap();
	private HashMap exporters = new HashMap();
	private ListenerList listeners = new ListenerList();
	
	public static ImageDescriptor createImage(String name) {
		return ImageDescriptor.createFromURL(makeImageURL(name));
	}

	private static URL makeImageURL(String name) {
		try {
			return Platform.resolve(Platform.find(getDefault().getBundle(), new Path("icons/" + name)));
		} catch (IOException e) {
			Log.logError("Can't find image with name " + name, e);
			return null;
		}
	}	

	public MetricsPlugin() {
		super();
		if (plugin == null) {
			plugin = this;
			try {
				resourceBundle= ResourceBundle.getBundle("net.sourceforge.metrics.core.MetricsPluginResources");
			} catch (MissingResourceException x) {
				resourceBundle = null;
			}
		}
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static MetricsPlugin getDefault() {
		return plugin;
	}
	
	/**
	 * get a list of all installed metric ids
	 * @return String[]
	 */
	public String[] getMetricIds() {
		if (ids == null) {
			ids = parsePrefString(false);
		}
		return ids;
	}
	
	/**
	 * get a list of all installed metric descriptions
	 * @return String[]
	 */
	public String[] getMetricDescriptions() {
		if (descriptions == null) {
			descriptions = parsePrefString(true);
		}
		return descriptions;
	}
	
	public MetricDescriptor getMetricDescriptor(String id) {
		return (MetricDescriptor) metrics.get(id);
	}
	
	private String[] parsePrefString(boolean description) {
		String stringList = getPluginPreferences().getString("METRICS.displayOrder");
		StringTokenizer t = new StringTokenizer(stringList, ",");
		int length = t.countTokens();
		String[] items = new String[length];
		for (int i = 0; i < length; i++) {
			String next = t.nextToken();
			int dash = next.indexOf('-');
			if (description) {
				items[i] = next.substring(dash+1).trim();
			} else {
				items[i] = next.substring(0,dash).trim();
			}
		}
		return items;
	}
	
	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle= MetricsPlugin.getDefault().getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
	public static long lastTimePreferencesChanged() {
		return getDefault().getPluginPreferences().getLong("METRICS.lastPrefChange");
	}
	
	/**
	 * record the last time the preferences were change to force recalculation
	 * after a change.
	 */
	public static void recordTimeAndClearCache() {
		//System.err.println("Recording preference change timestamp.");
		getDefault().getPluginPreferences().setValue("METRICS.lastPrefChange", String.valueOf(new Date().getTime()));
		//Cache.singleton.clear();
	}
	

	public static boolean isWarningsEnabled() {
		return getDefault().getPluginPreferences().getBoolean("METRICS.enablewarnings");
	}
	
	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(Preferences.PropertyChangeEvent event) {
		if (!event.getProperty().startsWith("METRICS")) {
			recordTimeAndClearCache();
		} else {
			ids = null;
			descriptions = null;
		}
		Object[] l = listeners.getListeners();
		for (int i = 0; i < l.length; i++) {
			if (l[i] instanceof Preferences.IPropertyChangeListener)
			((Preferences.IPropertyChangeListener)l[i]).propertyChange(event);
		}
	}

	private void installExtensions() {
		installMetrics();
		installExporters();
	}
	
	/**
	 * scan plugins for metrics and calculators and install them.
	 */
	private void installMetrics() {
		//System.err.println("Discovering and installing metrics");
		IExtensionPoint p = Platform.getExtensionRegistry().getExtensionPoint(pluginId + ".metrics");
		if (p != null) {
			IExtension[] x = p.getExtensions();
			for (int i = 0; i < x.length;i++) {
				IConfigurationElement[] elements = x[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					IConfigurationElement next = elements[j];
					String kind = next.getName();
					if (kind.equals("calculator")) {
						installCalculator(next);
					} else if (kind.equals("metric")) {
						installMetric(next);
					}
				}
			}
			initDisplayOrderPreference();
		}
	}
	
	private void installExporters() {
		//System.err.println("Discovering and installing metrics");
		IExtensionPoint p = Platform.getExtensionRegistry().getExtensionPoint(pluginId + ".exporters");
		if (p != null) {
			IExtension[] x = p.getExtensions();
			for (int i = 0; i < x.length;i++) {
				IConfigurationElement[] elements = x[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					IConfigurationElement next = elements[j];
					String kind = next.getName();
					if (kind.equals("exporter")) {
						installExporter(next);
					} 
				}
			}
		}
	}
	
	private void installExporter(IConfigurationElement element) {
		ExportDescriptor xd = ExportDescriptor.createFrom(element);
		if (xd != null) {
			exporters.put(xd.getClassName(), xd);
		}
	}

	public ExportDescriptor[] getExporters() {
		return (ExportDescriptor[])exporters.values().toArray(new ExportDescriptor[]{});
	}
	
	public ExportDescriptor getExporter(String className) {
		return (ExportDescriptor)exporters.get(className);
	}
	
	public IExporter getCurrentExporter() {
		String format = getPluginPreferences().getString("METRICS.xmlformat");
		return createExporter(format);
	}
	
	public boolean showProjectOnCompletion() {
		return getPluginPreferences().getBoolean("METRICS.showProject");
	}
	
	public IExporter createExporter(String className) {
		ExportDescriptor xd = getExporter(className);
		if (xd != null) {
			return xd.createExporter();
		}
		return null;
	}
	
	/**
	 * Set the default display order.
	 */
	private void initDisplayOrderPreference() {
		StringBuffer list = new StringBuffer();
		for (Iterator i = metrics.keySet().iterator();i.hasNext();) {
			String id = (String)i.next();
			String desc = ((MetricDescriptor)metrics.get(id)).getName();
			list.append(id).append(" - ").append(desc).append(',');
		}
		String def = list.substring(0,list.length()-1);
		getPluginPreferences().setDefault("METRICS.displayOrder", def);
		// if metrics were added/removed, reset the value
		if (getMetricIds().length != metrics.size()) {
			getPluginPreferences().setToDefault("METRICS.displayOrder");
		}
	}
	
	private void installMetric(IConfigurationElement element) {
		MetricDescriptor m = MetricDescriptor.createFrom(element);
		if (m != null) {
			if (m.isSum()) {
				// metric is not really a new metric, but merely the sum
				// of some other metric, make sure it gets calculated
				Sum s = new Sum(m.getId(), m.getSumOf());
				getCalculators(m.getLevel()).add(s);
				//System.err.println("Added a sum: " + s);
			}
			metrics.put(m.getId(), m);
			installPropagators(m);
			recordDependencies(m.getId(), m.getRequiredMetricIds());
		}
	}

	/**
	 * @param string
	 * @param strings
	 */
	private void recordDependencies(String id, String[] requires) {
		if (metricsDependencies == null) {
			metricsDependencies = new HashMap();
		}
		if (requires != null && requires.length > 0) {
			for (int i = 0; i < requires.length; i++) {
				List existing = (List)metricsDependencies.get(requires[i]);
				if (existing == null) {
					existing = new ArrayList();
				}
				existing.add(id);
				metricsDependencies.put(requires[i], existing);
			}
		}
	}

	private void installCalculator(IConfigurationElement nextCalculator) {
		CalculatorDescriptor c = createDescriptorFrom(nextCalculator);
		addCalculator(c);
	}
		
	private void addCalculator(CalculatorDescriptor c) {
		List calcs = getCalculators(c.getLevel());
		calcs.add(c.createCalculator());
		//System.err.println(c);
	}
	
	/**
	 * Install all propagators required by m
	 * @param m
	 */
	private void installPropagators(MetricDescriptor m) {
		String nextLevel = m.getParentLevel();
		if (nextLevel != null) {
			List props = m.createPropagators();
			for (Iterator i = props.iterator(); i.hasNext();) {
				Propagator p = (Propagator)i.next();
				getCalculators(nextLevel).add(p);
				propagatePropagator(nextLevel, p, m);
			}
			String newAvgMaxAt = m.getNewAvgMaxAt();
			if (newAvgMaxAt != null) {
				props = m.createIntroducedAvgMax();
				for (Iterator i = props.iterator(); i.hasNext();) {
					Propagator p = (Propagator)i.next();
					//System.err.println("Installing a " + p + " at level " + newAvgMaxAt);
					getCalculators(newAvgMaxAt).add(p);
					propagatePropagator(newAvgMaxAt, p, m);
				}
			}
		}
	}
	
	/**
	 * Create MaxMax, AvgAvg and Sum calculators at each level above
	 * the given level
	 * @param level
	 * @param p
	 */
	private void propagatePropagator(String level, Propagator p, MetricDescriptor m) {
		String nextLevel = MetricDescriptor.getNextLevel(level);
		Propagator nextP = p.createNextLevel();
		while (nextLevel != null) {
			getCalculators(nextLevel).add(nextP);
			//System.err.println("Added a " + nextP + " at level " + nextLevel);
			nextLevel = MetricDescriptor.getNextLevel(nextLevel);
			nextP = nextP.createNextLevel();
		}
	}
	
	/**
	 * Get the list of calculators for the given level
	 * @param level	as specified in the xml attribute level
	 * @return List
	 */
	public List getCalculators(String level) {
		List result = (List) calculators.get(level);
		if (result == null) {
			result = new ArrayList();
			calculators.put(level, result);
		}
		return result;
	}
	
	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop()
	 */
	public void stop(BundleContext context) throws Exception {
		Cache.singleton.close();
		super.stop(context);
	}
	
	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start()
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		installExtensions();
	}

	/**
	 * Method createCalculatorDescriptorFrom.
	 * @param iConfigurationElement
	 */
	private CalculatorDescriptor createDescriptorFrom(IConfigurationElement element) {
		return CalculatorDescriptor.createFrom(element);
	}
	
	/**
	 * @param listener
	 */
	public void addPropertyChangeListener(Preferences.IPropertyChangeListener listener) {
		listeners.add(listener);		
	}

	/**
	 * @param listener
	 */
	public void removePropertyChangeListener(Preferences.IPropertyChangeListener listener) {
		listeners.remove(listener);		
	}

	/**
	 * returns a list of metric ids that depend on the given metric being available
	 * @param descriptor
	 * @return array of metric ids or null if none depend on the given metric
	 */
	public String[] getDependentMetrics(MetricDescriptor descriptor) {
		if (descriptor == null) return null;
		List dependents = (List)metricsDependencies.get(descriptor.getId());
		if (dependents != null) return (String[]) dependents.toArray(new String[]{});
		return null;
	}
	
}

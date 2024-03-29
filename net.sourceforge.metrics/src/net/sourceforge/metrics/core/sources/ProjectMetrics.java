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
package net.sourceforge.metrics.core.sources;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.metrics.core.Log;
import net.sourceforge.metrics.core.MetricsPlugin;
import net.sourceforge.metrics.internal.xml.IXMLExporter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;


/**
 * initialize package fragment root metrics (my children) and collect all results
 * from the calculators for the project root level
 * @author Frank Sauer
 */
public class ProjectMetrics extends AbstractMetricSource implements IGraphContributor {

	static final long serialVersionUID = 7999821409641083884L;	
	
	private Map efferent;

	public ProjectMetrics() {
		super();
	}

	protected void initializeChildren() {
		IJavaProject pack = (IJavaProject)getJavaElement();
		try {
			IJavaElement[] children = pack.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IPackageFragmentRoot) {
					if (!((IPackageFragmentRoot)children[i]).isArchive()) {
						AbstractMetricSource next = Dispatcher.getAbstractMetricSource(children[i]);
						if (next != null) addChild(next);
						else Log.logMessage("Package " + children[i].getHandleIdentifier() + " not found.");
					}
				} 
			}
		} catch (JavaModelException e) {
		}
	
	}
	
	public void recurse() {
		initializeChildren();
		calculate();
		save();
	}
		
	/**
	 * @see net.sourceforge.metrics.core.sources.AbstractMetricSource#getCalculators()
	 */
	protected List getCalculators() {
		return MetricsPlugin.getDefault().getCalculators("project");
	}	
	
	/**
	 * @see net.sourceforge.metrics.core.sources.AbstractMetricSource#getLevel()
	 */
	public int getLevel() {
		return PROJECT;
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.metrics.core.sources.AbstractMetricSource#getExporter()
	 */
	public IXMLExporter getExporter() {
		return IXMLExporter.PROJECT_EXPORTER;
	}
	/* (non-Javadoc)
	 * @see net.sourceforge.metrics.core.sources.AbstractMetricSource#calculate()
	 */
	public void calculate() {
		super.calculate();
		mergeEfferentCouplings();
	}

	/**
	 * merge the efferent dependency graphs of my child sourcefolders into one
	 */
	private void mergeEfferentCouplings() {
		efferent = new HashMap();
		for (Iterator c = getChildren().iterator();c.hasNext();) {
			PackageFragmentRootMetrics next = (PackageFragmentRootMetrics) c.next();
			Map eff = next.getEfferent();
			addEfferent(eff);
		}
	}

	/**
	 * @param eff dependencies of a single source folder
	 */
	private void addEfferent(Map eff) {
		for (Iterator i = eff.keySet().iterator(); i.hasNext();) {
			String key = (String)i.next();
			Set deps = (Set)eff.get(key);
			if (deps != null) {
				Set total = (Set)efferent.get(key);
				if (total == null) {
					total = new HashSet();
					efferent.put(key, total);
				}
				total.addAll(deps);
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.metrics.core.sources.GraphContributor#getEfferent()
	 */
	public Map getEfferent() {
		return efferent;
	}

}

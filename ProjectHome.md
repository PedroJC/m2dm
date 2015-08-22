# Metamodel Driven Measurement (M2DM) Tool for Java #
## (Eclipse plugin for Java metrics formalization and collection) ##

This project was developed within the Software Systems Engineering group at the ISTAR Research Center at the ISCTE-IUL university in Lisbon, Portugal.

---

## Introduction ##
Metamodel Driven Measurement (M2DM) is a paradigm-independent approach to formalize and automatically collect metrics. The latter are defined as OCL queries over a metamodel representing the target domain. M2DM was proposed in 2001 by [Fernando Brito e Abreu](http://scholar.google.pt/citations?user=CCTGOawAAAAJ) and has been used since then within the [QUASAR research group](https://sites.google.com/site/quasarresearchgroup/) in diverse contexts such as UML modeling, component based development, aspect-oriented development, object-relational databases, IT service management or business process modeling (see QUASAR’s [publications](https://sites.google.com/site/quasarresearchgroup/ouractivity/publications) and [dissertation](https://sites.google.com/site/quasarresearchgroup/ouractivity/dissertations) pages for more details).

Due to the emergence of sanitized open source repositories, namely in Java, one of the [most popular programming languages](http://langpop.com/), the quest for [mining them for research purposes](http://www.msrconf.org/) has increased lately. Research endeavors of this kind require empirical validation and the latter implies defining explanatory and outcome variables. Those variables are expressed in this context by the so-called **software metrics**. Despite the fact that several object-oriented metrics suites have been proposed in the past (e.g. C&K or MOOD suites), no M2DM open-source tool for Java was available and we kept receiving requests worldwide for such a tool. Therefore, we decided to build one on top of Eclipse, the most popular open-source IDE currently used.
Since M2DM requires a metamodel of the target domain, we proposed the [EJMM (Eclipse Java Metamodel)](https://code.google.com/p/m2dm/source/browse/EJM2Metrics/metamodel/JavaMMv3_FLAME.use), based upon and instantiated through [Eclipse's Java Development Tools](http://www.eclipse.org/jdt/).

The provided M2DM plugin allows users to easily define new software metrics in OCL upon the EJMM. We have also ported FLAME (Formal Libray for Aiding Metrics Extraction) to the EJMM. FLAME was formerly proposed by Aline Baroni and Fernando Brito e Abreu upon the UML metamodel. Brito e Abreu’s MOOD and Chidamber and Kemerer metrics suites were formalized upon FLAME and are also made available.

## More Info and Citations ##
For more information on the internal details of the EJMM tool or for citation purposes, please refer to:
  * Pedro Janeiro Coimbra, Fernando Brito e Abreu, “[The Eclipse Java Metamodel: Scaffolding Software Engineering Research on Java Projects with Model-Driven Techniques](https://sites.google.com/site/quasarresearchgroup/ouractivity/publications)”, proceedings of the [2nd International Conference on Model-Driven Engineering and Software Development (MODELSWARD’2014)](http://www.modelsward.org/), Lisbon, Portugal, 7-9 January 2014.
  * Pedro Janeiro Coimbra,	"[An Eclipse Plugin for Metamodel Driven Measurement](https://sites.google.com/site/quasarresearchgroup/ouractivity/dissertations)", MSc dissertation, (supervised by Fernando Brito e Abreu), [University Institute of Lisbon (ISCTE-IUL)](http://www.iscte-iul.pt/), November 2013.


---

# Notes (version 0.8.5) #


## Installation ##
1. Put the EJM2Metrics\_0.8.5.jar file in your eclipse/dropins/plugins folder.

2. Run Eclipse. If the plug-in doesn't show, run Eclipse with the -clean command.


## Requirements ##
1. Make sure you have the EJMM USE specification file (.use).

2. Also make sure you have USE 3.0.6 installed.


## How to use ##
### Instantiation ###
1. Activate the Interactive view in Eclipse through Window > Show View > Other... (Note: the Interactive view is under the M2DM category)

2. Before instantiating the EJMM, you must first select your USE installation folder, using the "Select USE directory" button.

3. You must also select the location of your EJMM .use file using the "Select EJMM path" button.

4. Select the Java project to instantiate using the drop-down menu to the left. Only projects without errors can be instantiated, so make sure the project you want to analyze has no errors.

5. Press the "Instantiate EJMM" button to instantiate the EJMM with the selected Java project. Depending on the Java project size and its amount of code, the process may take several seconds.

### OCL querying ###
1. First, you must instantiate the EJMM by following the steps mentioned above.

2. The text box under "Insert OCL queries here:" will activate once the EJMM has been successfully instantiated.

3. You may input any OCL query on this text box and press the return key (Enter) to process the query. It functions exactly like the OCL expression evaluator in USE.

4. Results are shown in the large text area in the bottom of the view.

## Notes ##
1. If you are still having trouble installing the plug-in, try putting the EJM2Metrics\_0.8.5.jar file in the eclipse/dropins folder or the eclipse/plugins folder.

2. If you're having trouble instantiating large Java projects, you might want to try increasing the JVM heap space.
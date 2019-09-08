import java.awt.Color;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.eclipse.epsilon.emc.simulink.model.SimulinkModel;
import org.eclipse.epsilon.emc.simulink.model.element.ISimulinkModelElement;
import org.eclipse.epsilon.eol.exceptions.EolIllegalPropertyException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.layout.HierarchicalLayout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.CloseFramePolicy;
import org.leonardo.swt.swing_compat.EmbeddedSwingComposite;

public class LeonardoDigitalThread {
	private Graph graph;

	static String readFileWithDefault(String path, String defaultedValue) {
		try {
			return readFile(path, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
			return defaultedValue;
		}
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(new File(path).toPath());
		return new String(encoded, encoding);
	}

	static final String CSS = readFileWithDefault(LeonardoDigitalThread.class.getResource("style.css").getFile(), "");

	class GraphView{
		Viewer viewer;
		ViewPanel vP_new;

		public GraphView(Viewer viewer, ViewPanel vP_new) {
			this.viewer = viewer;
			this.vP_new = vP_new;
		}
	}

	static ViewPanel createGraphView(Graph graph, boolean autoLayout) {
		Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
		if(autoLayout) {
			viewer.enableAutoLayout();
		}
		boolean openIfFrame = false;
		ViewPanel vP_new = viewer.addDefaultView(openIfFrame);
		viewer.setCloseFramePolicy(CloseFramePolicy.CLOSE_VIEWER);
		return vP_new;
	}

	public static void main(String[] args) {

		SimulinkModel model = new SimulinkModel();
		model.setName("SimulinkModel");
		String libraryPath = "C:\\Program Files\\MATLAB\\R2019a\\bin\\win64";
		String engineJarPath = "C:\\Program Files\\MATLAB\\R2019a\\extern\\engines\\java\\jar\\engine.jar";
		String workingDir = "D:\\\\simscapeExperiment1";
		//String modelfile = "D:\\simscapeExperiment1\\simple.slx";
		String modelfile = "D:\\simscapeExperiment1\\tank_circuit.slx";
		//String modelfile = "D:\\\\simscapeExperiment1\\ee_radio_am_receiver.slx";
		boolean followLinks = true;
		boolean openMatlabEditor = false;

		model.setWorkingDir(new File(workingDir));
		model.setFollowLinks(followLinks);
		model.setShowInMatlabEditor(openMatlabEditor);
		model.setLibraryPath(libraryPath);
		model.setEngineJarPath(engineJarPath);
		model.setFile(new File(modelfile));
		try {
			model.load();
		} catch (EolModelLoadingException e) {
			e.printStackTrace();
		}
		final MultiGraph graphNew = new MultiGraph("Graph");
		graphNew.addAttribute("ui.quality");
		graphNew.addAttribute("ui.antialias");
		graphNew.addAttribute("ui.stylesheet", CSS);
		
		//simulinkToGraph1(model, graphNew);
		simulinkToGraph2(model, graphNew);

		System.out.println("Finished Simulink Work!");

		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(1, false));
		EmbeddedSwingComposite embeddedComposite = new EmbeddedSwingComposite(shell, SWT.NONE) {
			protected JComponent createSwingComponent() {
				JScrollPane scrollPane = new JScrollPane();
				ViewPanel viewPanel = createGraphView(graphNew, true);
				scrollPane.setViewportView(viewPanel);
				return scrollPane;
			}
		}; 
		embeddedComposite.populate();
		embeddedComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		shell.open();
		// run the event loop as long as the window is open
		while (!shell.isDisposed()) {
			// read the next OS event queue and transfer it to a SWT event
			if (!display.readAndDispatch()) {
				// if there are currently no other OS event to process sleep until the next OS event is available
				display.sleep();
			}
		}
		display.close();
		System.exit(0);
	}

	private static void simulinkToGraph1(final SimulinkModel model, final MultiGraph graphNew) {
		Map<String, Node> mapNodes = new HashMap<String, Node>();
		Map<String, Edge> mapEdges = new HashMap<String, Edge>();
		for(ISimulinkModelElement x : model.allContents()) {
			try {
				try {
					Node n1 = mapNodes.get(x.getPath());
					try {
						Object parent = x.getProperty("Parent");

						Object parameterList = x.getProperty("ObjectParameters");

						System.out.println("Skip " + parameterList.getClass());

						org.eclipse.epsilon.emc.simulink.types.Struct list = ( org.eclipse.epsilon.emc.simulink.types.Struct) parameterList;
						Set<Entry<String, Object>> set = (Set<Entry<String, Object>>)list.entrySet();
						for(Entry<String, Object> listEle : set) {
							System.out.println("\t\t" + listEle.getKey() + " :: ");
							for(Entry<String, Object> listValEle : ((com.mathworks.matlab.types.Struct)listEle.getValue()).entrySet()) {
								System.out.println("\t\t\t" + listValEle.getKey() + " :: " + listValEle.getValue());
								if(listValEle.getValue() instanceof String[]) {
									for(String strVal : (String[])listValEle.getValue()) {
										System.out.println("\t\t\t\t" + strVal);
									}
								}
							}
							System.out.println("\t\t Value: " + x.getProperty(listEle.getKey()));	

						}

						//Object parentObj = model.getElementById((String) parent);
						//System.out.println(parent + " " + parentObj);
					} catch (EolIllegalPropertyException e1) {
						e1.printStackTrace();
					}
					if(n1 == null) {
						n1 = graphNew.addNode(x.getPath());
						n1.addAttribute("ui.label", x.getPath());
						mapNodes.put(x.getPath(), n1);
					}
					Node typeNode = mapNodes.get(x.getType());
					if(typeNode == null) {
						typeNode = graphNew.addNode(x.getType());
						typeNode.addAttribute("ui.label", x.getType());
						mapNodes.put(x.getType(), typeNode);
					}

					Edge e = mapEdges.get(x.getPath()+"_"+x.getType());
					if(e == null) {
						e = graphNew.addEdge(x.getPath()+"_"+x.getType(), n1, typeNode);
						mapEdges.put(x.getPath()+"_"+x.getType(), e);
					}
					System.out.println(x.hashCode());
					System.out.println(x.getPath());
				}catch(java.lang.NullPointerException nPE) {
					System.err.println("Got null but continue " + x.getType());
				}

			}catch(java.lang.NullPointerException nPE) {
				System.err.println("Got null again but uuuuh keep going...");
			}
		}
		/*try {
			for(ISimulinkModelElement x : model.getAllOfType("SubSystem")) {
				System.out.println(x);
			}
		} catch (EolModelElementTypeNotFoundException e) {
			e.printStackTrace();
		}*/

		/*Node nH1 = graphNew.addNode("Component Hello 1");
		Node nH2 = graphNew.addNode("Component Hello 2");
		Node nH3 = graphNew.addNode("Component Hello 3");
		Node nH3a = graphNew.addNode("Component Hello 3a");
		Node nH3b = graphNew.addNode("Component Hello 3b");
		graphNew.addEdge("e1", n1, nH1);
		graphNew.addEdge("e2", n1, nH2);
		graphNew.addEdge("e3", n1, nH3);
		graphNew.addEdge("e3a", nH3, nH3a);
		graphNew.addEdge("e3b1", nH3, nH3b);
		graphNew.addEdge("e3b2", nH3b, n1);*/
	}
	private static void simulinkToGraph2(final SimulinkModel model, final MultiGraph graphNew) {
		Map<String, Node> mapNodes = new HashMap<String, Node>();
		Map<String, Edge> mapEdges = new HashMap<String, Edge>();
		for(ISimulinkModelElement x : model.allContents()) {
			try {
				try {
					try {
						Object parent = x.getProperty("Parent");

						Object parameterList = x.getProperty("ObjectParameters");

						System.out.println("Skip " + parameterList.getClass());

						org.eclipse.epsilon.emc.simulink.types.Struct list = ( org.eclipse.epsilon.emc.simulink.types.Struct) parameterList;
						Set<Entry<String, Object>> set = (Set<Entry<String, Object>>)list.entrySet();
						for(Entry<String, Object> listEle : set) {
							System.out.println("\t\t" + listEle.getKey() + " :: ");
							for(Entry<String, Object> listValEle : ((com.mathworks.matlab.types.Struct)listEle.getValue()).entrySet()) {
								System.out.println("\t\t\t" + listValEle.getKey() + " :: " + listValEle.getValue());
								if(listValEle.getValue() instanceof String[]) {
									for(String strVal : (String[])listValEle.getValue()) {
										System.out.println("\t\t\t\t" + strVal);
									}
								}
							}
							System.out.println("\t\t Value: " + x.getProperty(listEle.getKey()));	

						}

						//Object parentObj = model.getElementById((String) parent);
						//System.out.println(parent + " " + parentObj);
					} catch (EolIllegalPropertyException e1) {
						e1.printStackTrace();
					}
					Node n1 = mapNodes.get(x.getPath());
					if(n1 == null) {
						n1 = graphNew.addNode(x.getPath());
						n1.addAttribute("ui.label", x.getPath());
						mapNodes.put(x.getPath(), n1);
					}
					Node typeNode = mapNodes.get(x.getType());
					if(typeNode == null) {
						typeNode = graphNew.addNode(x.getType());
						typeNode.addAttribute("ui.label", x.getType());
						mapNodes.put(x.getType(), typeNode);
					}

					Edge e = mapEdges.get(x.getPath()+"_"+x.getType());
					if(e == null) {
						e = graphNew.addEdge(x.getPath()+"_"+x.getType(), n1, typeNode);
						mapEdges.put(x.getPath()+"_"+x.getType(), e);
					}
					System.out.println(x.hashCode());
					System.out.println(x.getPath());
				}catch(java.lang.NullPointerException nPE) {
					System.err.println("Got null but continue " + x.getType());
				}

			}catch(java.lang.NullPointerException nPE) {
				System.err.println("Got null again but uuuuh keep going...");
			}
		}
	}
}

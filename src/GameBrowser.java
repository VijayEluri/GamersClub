/**
 * Class GameBrowser
 *
 * Makes Main Menu and redirects to other panels
 *
 * @author Leon Blakey/Lord.Quackstar
 */
import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.text.DateFormat;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;

import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout;

import org.jdesktop.swingx.MultiSplitLayout.Node;

import org.json.me.JSONArray;
import org.json.me.JSONObject;

public class GameBrowser extends JXMultiSplitPane implements ActionListener,TreeSelectionListener{
	JPanel picPanel,downPanel;
	JScrollPane descPanel;
	JTextArea descPane;
	Path gameDir;
	boolean valueChangedRunning = false;
	int valueHash;
	TreeMap<String,Map> gameData = new TreeMap<String,Map>();
	String layout;
	
	/*Returns a completed JPanel for master class*/
	public JXMultiSplitPane generate() {
		System.out.println("GameBrowser Class initiated");
		
		removeAll(); //Might get recalled, so clear panel first
		

		
		/***Configure layout***/	
		layout = "(ROW (LEAF name=sidebar weight=0.35) (COLUMN weight=0.65 (ROW weight=0.6 (LEAF name=pic weight=0.25) (LEAF name=desc weight=0.75)) (LEAF name=download weight=0.4)))))";
		getMultiSplitLayout().setModel(MultiSplitLayout.parseModel(layout));
		add(buildTreeMenu(),"sidebar");
		add(picPanel = new JPanel(),"pic");
		add(downPanel = new JPanel(),"download");
		picPanel.add(new JLabel("Please Select a Game",JLabel.CENTER));
		
		/***Config descPane for panel***/
		descPane = new JTextArea(10,35);
		descPane.setEditable(false);
		descPane.setBorder(null); 
		descPane.setLineWrap(true);
		descPane.setWrapStyleWord(true);
		descPane.setBackground(this.getBackground());
		add(descPanel = new JScrollPane(descPane),"desc");
		
		picPanel.setLayout(new BoxLayout(picPanel,BoxLayout.Y_AXIS));
		downPanel.setLayout(new BoxLayout(downPanel,BoxLayout.Y_AXIS));
		downPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)); 
		descPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		
		return this;
	}
	
	private JPanel buildTreeMenu() {
        //Create the nodes.
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Game Broser");
		
        //Create a tree that allows one selection at a time.
        JTree tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(this);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
		
        //Listen for when the selection changes.
        tree.addTreeSelectionListener(this);
        
        /***Configure Pane***/
        JScrollPane treeView = new JScrollPane(tree);
        JPanel treePanel = new JPanel();
        treePanel.setLayout(new BoxLayout(treePanel,BoxLayout.Y_AXIS));
        treePanel.add(treeView);
        JButton addGame = new JButton ("Add Game");
        addGame.addActionListener(this);
        treePanel.add(addGame);
        
        
        /***Start adding nodes***/
		try {
			DefaultMutableTreeNode category = null;
        	DefaultMutableTreeNode book = null;
		
			System.out.println("Asking server for game list");
			String response = Globs.webTalk("mode=buildTree",null,null);
			//System.out.println("Response: "+response);
			
			//use modified JSONObject to return hashmap iterator
			Iterator dbIter = new JSONObject(response).myHashMap.entrySet().iterator(); 
			
			//Init values
			Map.Entry typeRow = null;
			Iterator typeIter = null;
			String gameRow = "";
			String key = "";
			
			while(dbIter.hasNext()) {
				//currently looping through game types
				typeRow = (Map.Entry)dbIter.next();
				
				//Add key to big array
				key = (String)typeRow.getKey();
				top.add(category = new DefaultMutableTreeNode(key));
				System.out.println("typeRow value string: "+key);
				
				//Note: JSONArray stores values in Vector, not Map
				typeIter = ((JSONArray)typeRow.getValue()).myArrayList.iterator();
				
				while(typeIter.hasNext()) {
					//currently looping through games in game types
					gameRow = (String)typeIter.next();
					
					//Now we have excaped game info json string, need to unescape it
					gameRow = gameRow.replace("\\\"","\"");
					Hashtable gameValue = new JSONObject((String)gameRow).myHashMap;
					
					//Make a TreeMap to add to big class TreeMap
					String name = gameValue.get("name").toString();
					gameData.put(name,(Map)gameValue);
					
					//Make the tree node
					category.add(new DefaultMutableTreeNode(name));
					System.out.println("Name: "+name);
				}
			}
		}
		catch(Exception e) {
      		e.printStackTrace();
      	}
      	
      	//Make it expand
      	tree.expandPath(new TreePath(top.getPath()));
		return treePanel;
    }
    
    /** Required by TreeSelectionListener interface. */
    public void valueChanged(TreeSelectionEvent e) {
    	DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.getNewLeadSelectionPath().getLastPathComponent();
    	int currentHash = e.hashCode();
    	
    	if(valueHash == currentHash) {
    		System.out.println("ValueHash: Already running, exit");
    		return; //already running
    	}
    	else {
    		System.out.println("ValueHash: Not running. Setting var as true and continuing");
    		valueHash=currentHash;
    	}
    	
    	//is anything selected?
    	if (node == null)
    		return;

    	Object nodeInfo = node.getUserObject();
    	if (node.isLeaf()) {
        	System.out.println("Leaf Selected: "+nodeInfo);
        	
        	//Get resultset
        	Map gameInfo = gameData.get(nodeInfo.toString());
            
            /***Setup desccription pane***/
        	String desc = gameInfo.get("desc").toString();
        	System.out.println(desc);
		    descPane.setText(desc);
		      	
		    /***Setup Picture Pane***/
		    picPanel.removeAll();
		    JLabel gamePic = new JLabel("",JLabel.CENTER);
		    Path picPath = Paths.get(gameInfo.get("picture").toString()); 
		    ImageIcon gamePicIcon;
			if(picPath.exists()!=true) {
			   	System.out.println("Game provided image not avalible");
			   	gamePicIcon = new ImageIcon("noImage.jpg");
			}
			else {
			   	gamePicIcon = new ImageIcon(picPath.toString());
			}
			gamePic.setIcon(gamePicIcon);  
			picPanel.add(gamePic);
			//add dates
			picPanel.add(new JLabel("<HTML><b>Date Added:</b> " + DateFormat.getDateInstance(DateFormat.FULL).format(Long.valueOf(gameInfo.get("addDate").toString().trim().replace("\\/","/"))) + "</HTML>"));
			picPanel.add(new JLabel("<HTML><b>Date Game Created:</b> " + DateFormat.getDateInstance(DateFormat.FULL).format(Long.valueOf(gameInfo.get("createDate").toString().trim())) + "</HTML>"));
			
			/***Setup Download Pane***/
			downPanel.removeAll();
			
			JLabel downLabel = new JLabel("Select which type you wish to download: ",JLabel.CENTER);
			downLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			downPanel.add(downLabel);
			JPanel buttonList = new JPanel();
			try {
				Iterator downJSON = new JSONObject(gameInfo.get("dirs").toString()).myHashMap.entrySet().iterator();
				while(downJSON.hasNext()) {
					Map.Entry currentDir = (Map.Entry)downJSON.next();
					JButton downButton = new JButton(currentDir.getKey().toString());
					downButton.setActionCommand(currentDir.getValue().toString());
					downButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							//Simply pass control to PasteGame
							GamersClub.PasteGame.config(e.getActionCommand());
						}
					});
					buttonList.add(downButton);
				}
				downPanel.add(buttonList);
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
    	} 
    	else {
        	System.out.println("NodeList Selected: "+nodeInfo); 
    	}
    	repaint();
		revalidate();
		System.out.println("Running");
		this.valueChangedRunning = false;
		getMultiSplitLayout().setModel(MultiSplitLayout.parseModel(layout));
    }
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if(cmd.equals("Add Game")) {
			GamersClub.AddGame.generate();
			Globs.switchBody("AddGame");
		}
    }
}

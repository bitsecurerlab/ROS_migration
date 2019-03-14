package nodecluster;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeUtils;

import similarity.DTree;
import similarity.Split;

public class Utils {
	private static Stack<String> blocks = new Stack<>();
	
	public static void main (String args[]) throws Exception{
		String path = "talker.cpp";
		File cppfile = new File(path);
		TreeContext tc1 = new SrcmlCppTreeGenerator().generateFromFile(cppfile);
		String path2 = "talker2.cpp";
		File cppfile2 = new File(path2);
		TreeContext tc2 = new SrcmlCppTreeGenerator().generateFromFile(cppfile2);		
//		HashMap<String, LinkedList<Action>> actions = collectAction(tc1, tc2);
//		printAllActions(tc1, tc2, actions);
		String miName = path2.split("/")[path2.split("/").length-1];//����ļ���
		Split sp = new Split();
		ArrayList<SubTree> sts = sp.splitSubTree(tc2, miName);
		System.out.println(sts.size());
		for(SubTree st : sts) {
			String src = subtree2src(st);
			System.out.println(src);
		}
	}
	
	public static String subtree2src(SubTree st) throws Exception {
		String src = "";
		String loopEnd = "";
		ITree root = st.getRoot();
		TreeContext srcT = st.getTC();
		String sType = srcT.getTypeLabel(root);
		if(sType.equals("while")||sType.equals("for")) {
			if(sType.equals("while"))
				src = src+"while(";
			if(sType.equals("for"))
				src = src+"for(";
			loopEnd = ")";
		}
		
		List<ITree> leaves = new ArrayList<>();
		leaves = traverse2Leaf(root, leaves);
//		System.out.println(leaves.size());
//		for(ITree leaf : leaves) {
//			String type = srcT.getTypeLabel(leaf);
//			String label = leaf.getLabel();
//			System.out.println(type+":"+label);
//		}
		if(leaves.size()==0)
			throw new Exception("null leaves");
		else if(leaves.size()==1) {
			src = src+leaves.get(0).getLabel();//�Ȱ�0��Ҷ�ӷ���
			return src;
		}
		
		src = src+leaves.get(0).getLabel();//�Ȱ�0��Ҷ�ӷ���
		for(int i=0;i<leaves.size()-1;i++) {
			int size = 0;
			ITree leaf1 = leaves.get(i);
			ITree leaf2 = leaves.get(i+1);
			if(leaf1.isRoot()||leaf2.isRoot())//Ҷ�ӽڵ�Ϊ�������ڵ㣬����ô��
				throw new Exception("why is root???");
			ITree sharePar = findShareParent(leaf1, leaf2, srcT);
//			String parType = srcT.getTypeLabel(sharePar);
			List<ITree> childs = sharePar.getChildren();
			if(childs.contains(leaf1)&&childs.contains(leaf2)) {//ͬһ�������Ҷ�ӽڵ㣬��ԭʱ��ֱ��ƴ��������
				src = src+ leaf2.getLabel();
			}else if(childs.size()>=2){//��������۲�ͬ��֧�»�ԭ��������
				ITree node1 = null;
				ITree node2 = null;
				for(ITree child : childs) {
					if(child.isLeaf()) {
						if(child.equals(leaf1))
							node1 = child;
						if(child.equals(leaf2))
							node2 = child;
					}else {
						List<ITree> list = TreeUtils.preOrder(child);
						if(list.contains(leaf1))
							node1 = child;
						if(list.contains(leaf2))
							node2 = child;
//						if(list.contains(leaf1)&&list.contains(leaf1))
//							throw new Exception("wrong sharePar!");
					}				
				}//��sharePar����һ��leaf1,leaf2��Ӧ���ڵ�(���䱾��)
				String type1 = srcT.getTypeLabel(node1);
				String type2 = srcT.getTypeLabel(node2);
				if(type1.equals("name")) {
					if(type2.equals("argument_list")||type2.equals("parameter_list")) {						
						List<ITree> arguLeaves = new ArrayList<>();
						arguLeaves = traverse2Leaf(node2, arguLeaves);//�ҵ�argulist������Ҷ��
						src = src + recoverArguList(node2, arguLeaves, srcT);//argulist��������
						size = arguLeaves.size();
						i=i+size-1;
					}else if(type2.equals("init")) {
						src = src+"="+leaf2.getLabel();
					}else if(type2.equals("operator")) {
						src = src+leaf2.getLabel();
					}else
						throw new Exception("û���ǹ���name���:"+type2);
				}else if(type1.equals("type")) {
					if(type2.equals("name")) {	
						src = src+" "+leaf2.getLabel();
					}else
						throw new Exception("û���ǹ���type���:"+type2);
				}else if(type1.equals("operator")) {
					if(type2.equals("call")) {//������node2Ϊcall�����
						node2 = node2.getChildren().get(0);
						type2 = srcT.getTypeLabel(node2);
					}						
					if(type2.equals("name")||type2.equals("operator")) {
						src = src+leaf2.getLabel();
					}else						
						throw new Exception("û���ǹ���operator���:"+type2);
				}else if(type1.equals("specifier")) {
					if(type2.equals("name")){
						src = src+" "+leaf2.getLabel();
					}else
						throw new Exception("û���ǹ���type���:"+type2);
				}
			}else						
				throw new Exception("û���ǹ���children���");
		}
		src = src+loopEnd;
		return src;		
	}
	
	public static String recoverArguList(ITree root, List<ITree> arguLeaves, TreeContext srcT) throws Exception {
		String arguSrc = "";
		String end = "";
		ITree node = root.getParent();
		String type = srcT.getTypeLabel(node);//�ҵ�argument_list���ڵ�
		if(type.equals("name")) {//name�����<>
			arguSrc = arguSrc+"<";
			end = ">";
		}else if(type.equals("call")||type.equals("decl")) {//call���������()
			arguSrc = arguSrc+"(";
			end = ")";
		}else if(type.equals("constructor")||type.equals("function")) {
			arguSrc = arguSrc+"(";
			end = ")";
		}			
		if(arguLeaves.size()==0) {
			arguSrc = arguSrc+end;
			return 	arguSrc;
		}//���ؿ�����
		if(arguLeaves.size()==1) {
			arguSrc = arguSrc + arguLeaves.get(0).getLabel()+end;
			return 	arguSrc;
		}//���ص���Ԫ��+����
				
		arguSrc = arguSrc + arguLeaves.get(0).getLabel();
		for(int i=0;i<arguLeaves.size()-1;i++) {
			ITree leaf1 = arguLeaves.get(i);
			ITree leaf2 = arguLeaves.get(i+1);
			ITree sharePar = findShareParent(leaf1, leaf2, srcT);
//			String parType = srcT.getTypeLabel(sharePar);
			List<ITree> childs = sharePar.getChildren();
			if(childs.contains(leaf1)&&childs.contains(leaf2)) {//ͬһ�������Ҷ�ӽڵ㣬��ԭʱ��ֱ��ƴ��������
				arguSrc = arguSrc+ leaf2.getLabel();
			}else if(childs.size()>=2){
				ITree node1 = null;
				ITree node2 = null;
				for(ITree child : childs) {
					if(child.isLeaf()) {
						if(child.equals(leaf1))
							node1 = child;
						if(child.equals(leaf2))
							node2 = child;
					}else {
						List<ITree> list = TreeUtils.preOrder(child);
						if(list.contains(leaf1))
							node1 = child;
						if(list.contains(leaf2))
							node2 = child;
//						if(list.contains(leaf1)&&list.contains(leaf1))
//							throw new Exception("wrong sharePar!");
					}				
				}//��sharePar����һ��leaf1,leaf2��Ӧ���ڵ�(���䱾��)
				String type1 = srcT.getTypeLabel(node1);
				String type2 = srcT.getTypeLabel(node2);
				if(type1.equals("name")) {
					if(type2.equals("argument_list")||type2.equals("parameter_list")) {	
						List<ITree> leaves = new ArrayList<>();
						leaves = traverse2Leaf(node2, leaves);//�ҵ�argulist������Ҷ��
						arguSrc = arguSrc + recoverArguList(node2, leaves, srcT);
					}else if(type2.equals("operator")) {
						arguSrc = arguSrc+leaf2.getLabel();
					}else
						throw new Exception("û���ǹ���name���:"+type2);					
				}else if(type1.equals("argument")||type1.equals("parameter")) {
					if(type2.equals("argument")||type2.equals("parameter")) {
						arguSrc = arguSrc+","+leaf2.getLabel();
					}else
						throw new Exception("û���ǹ���argument���:"+type2);
				}else if(type1.equals("type")) {
					if(type2.equals("name")) {	
						arguSrc = arguSrc+" "+leaf2.getLabel();
					}else
						throw new Exception("û���ǹ���type���:"+type2);
				}else if(type1.equals("operator")) {
					if(type2.equals("call")) {//������node2Ϊcall�����
						node2 = node2.getChildren().get(0);
						type2 = srcT.getTypeLabel(node2);
					}						
					if(type2.equals("name")||type2.equals("operator")) {
						arguSrc = arguSrc+leaf2.getLabel();
					}else						
						throw new Exception("û���ǹ���operator���:"+type2);
				}else if(type1.equals("specifier")) {
					if(type2.equals("name")){
						arguSrc = arguSrc+" "+leaf2.getLabel();
					}else
						throw new Exception("û���ǹ���type���:"+type2);
				}else						
					throw new Exception("û���ǹ���children���");
			}		
		}
		arguSrc = arguSrc+end;//������β
		return arguSrc;		
	}//argulist�൱��subtree�е�subtree��������ԭ
	
	
	public static ITree findShareParent(ITree leaf1, ITree leaf2, TreeContext tc) throws Exception {
		if(leaf1.isRoot()||leaf2.isRoot()) 
			throw new Exception("check the leaf!");
		ITree sharePar = null;
		if(leaf1.getParent().equals(leaf2.getParent()))//�����ͬ������
			sharePar = leaf1.getParent();
		else {
			ITree subRoot = traverSubRoot(leaf1, tc);
			Boolean ifSamePar = true;
			while(ifSamePar) {
				List<ITree> children = subRoot.getChildren();
				for(ITree child : children) {
					List<ITree> order = TreeUtils.preOrder(child);
					if(order.contains(leaf1)&&order.contains(leaf2)) {
						ifSamePar = true;
						subRoot = child;
						break;
					}						
					else 
						ifSamePar = false;
				}				
			}
			sharePar = subRoot;
		}
//		System.out.println("find sharePar:"+tc.getTypeLabel(sharePar));
		return sharePar;		
	}//�������ڵ㹲����͵ĸ��׽ڵ�	

	
	public static ITree traverSubRoot(ITree node, TreeContext tc) {
		ITree subRoot = null;
		String typeLabel = tc.getTypeLabel(node);				
		while(!ifSRoot(typeLabel)) {//���������⣬Ҫע��ѭ������
			if(node.isRoot()) {//�������޸�include�������subrootΪ�������ڵ�
				subRoot = node;
				break;
			}else {
				node = node.getParent();
				typeLabel = tc.getTypeLabel(node);
//				System.out.println("typeLabel:"+typeLabel);
				subRoot = node;
			}		
		}
		return subRoot;
	}//����׷��ĳ�ڵ��������ڵĸ��ڵ�
	
	public static List<ITree> collectNode(ITree node, List<ITree> nodes) throws Exception {
		if(nodes.isEmpty())
			nodes.add(node);
		if(node.isRoot()&&node.getChildren().isEmpty())
			throw new Exception("Empty root");
		List<ITree> childs = node.getChildren();
		nodes.addAll(node.getChildren());
		for(ITree child : childs) {
			if(!child.getChildren().isEmpty()) {
				collectNode(child, nodes);
			}else continue;
		}
		return nodes;		
	}//�ռ�AST�������е�
	
	public static Integer collectEdge(ITree node, int num) {
		List<ITree> childs = node.getChildren();
		for(ITree child : childs) {
			num = num+1;
//			System.out.println(child.getParent().getId()+"->"+child.getId());
			collectEdge(child, num);
		}
		return num;		
	}//�ռ�AST�������б�
	
	public static ArrayList<Integer> collectSrcActNodeIds(TreeContext tc1, TreeContext tc2, HashMap<String, LinkedList<Action>> actMap) throws Exception{
		ArrayList<Integer> srcActIds = new ArrayList<>();
        HashMap<String, LinkedList<Action>> actions = actMap;
        LinkedList<Action> updates = actions.get("update");
        LinkedList<Action> deletes = actions.get("delete");
        LinkedList<Action> inserts = actions.get("insert");
        LinkedList<Action> moves = actions.get("move");
		
		Cluster cl = new Cluster(tc1, tc2);
		for(Action a : updates) {
			int id = a.getNode().getId();
			srcActIds.add(id);
		}
		
		for(Action a : deletes) {
			int id = a.getNode().getId();
			srcActIds.add(id);
		}
		
		for(Action a : inserts) {			
			ITree sRoot = cl.traverseSRoot(a);			
			int id = sRoot.getId();
			srcActIds.add(id);
		}
		
		for(Action a : moves) {
			ITree sRoot = cl.findMovRoot(a);			
			int id = sRoot.getId();
			srcActIds.add(id);
		}
		return srcActIds;        
	}	
	
	public static String printLeaf(DTree st) throws Exception {
		String values = "";
		List<ITree> leaves = st.getChildren();
		for(ITree leaf : leaves) {
			values = values + leaf.getLabel();
		}		
		return values;		
	}
	
	public static List<ITree> traverse2Leaf(ITree node, List<ITree> leafList) throws Exception{//�Ӹ��ڵ���ȱ�����Ҷ�ӽڵ㣬����ȷ��path��ͬ��leaf mappings
		List<ITree> childList = node.getChildren();
		if(node.isLeaf()){
			leafList.add(node);
		}else{
			for(ITree child : childList){
				leafList = traverse2Leaf(child, leafList);
			}
		}
		return leafList;
	}
	
	public static HashMap<String, LinkedList<Action>> collectAction(TreeContext tc1, TreeContext tc2) {
		Matcher m = Matchers.getInstance().getMatcher(tc1.getRoot(), tc2.getRoot());
        m.match();
        ActionGenerator g = new ActionGenerator(tc1.getRoot(), tc2.getRoot(), m.getMappings());
        List<Action> actions = g.generate();
        System.out.println("ActionSize:"+actions.size());
        HashMap<String, LinkedList<Action>> actionMap = new HashMap<>();
        HashMap<Integer, Action> moves = new HashMap<Integer, Action>();
        HashMap<Integer, Action> updates = new HashMap<Integer, Action>();
        HashMap<Integer, Action> inserts = new HashMap<Integer, Action>();
        HashMap<Integer, Action> deletes = new HashMap<Integer, Action>();
        ArrayList<Integer> movId = new ArrayList<>();
        ArrayList<Integer> uptId = new ArrayList<>();
        ArrayList<Integer> addId = new ArrayList<>();
        ArrayList<Integer> delId = new ArrayList<>();
        LinkedList<Action> mov = new LinkedList<>();
        LinkedList<Action> upt = new LinkedList<>();
        LinkedList<Action> add = new LinkedList<>();
        LinkedList<Action> del = new LinkedList<>();
        for (Action a : actions) {
            ITree src = a.getNode();
            if (a instanceof Move) {
                moves.put(src.getId(), a);
                movId.add(src.getId());
//                System.out.println(((Move)a).toString());
            } else if (a instanceof Update) {
            	updates.put(src.getId(), a);
            	uptId.add(src.getId());
//                System.out.println(((Update)a).toString());
            } else if (a instanceof Insert) {
            	inserts.put(src.getId(), a);
            	addId.add(src.getId());
//                System.out.println(((Insert)a).toString());
            } else if (a instanceof Delete) {
            	deletes.put(src.getId(), a);
            	delId.add(src.getId());
//            	System.out.println(((Delete)a).toString());
            }           
         }
        for(int n : movId) {
        	Action tmp = moves.get(n);
        	mov.add(tmp);
        }
        for(int n : uptId) {
        	Action tmp = updates.get(n);
        	upt.add(tmp);
        }
        for(int n : addId) {
        	Action tmp = inserts.get(n);
        	add.add(tmp);
        }
        for(int n : delId) {
        	Action tmp = deletes.get(n);
        	del.add(tmp);
        }
        actionMap.put("move", mov);
        actionMap.put("update", upt);
        actionMap.put("insert", add);
        actionMap.put("delete", del);
        
        return actionMap;
	}
	
	public static void printAllActions(TreeContext tc1, TreeContext tc2, HashMap<String, LinkedList<Action>> actionMap) {
		Matcher m = Matchers.getInstance().getMatcher(tc1.getRoot(), tc2.getRoot());
        m.match();
        
        for(Mapping map : m.getMappings()) {
        	ITree src = map.getFirst();
        	ITree dst = map.getSecond();
        	System.out.println("Mapping:"+src.getId()+"->"+dst.getId());
        }                  
        
        LinkedList<Action> moves = actionMap.get("move");
        LinkedList<Action> updates = actionMap.get("update");
        LinkedList<Action> inserts = actionMap.get("insert");
        LinkedList<Action> deletes = actionMap.get("delete");
        for(int i=0;i<moves.size();i++) {
        	Move act = (Move)moves.get(i);
        	System.out.println("Mov:"+act.getNode().getId()+"->"+act.getParent().getId()+","+act.getPosition());
        }
        for(int i=0;i<updates.size();i++) {
        	Update act = (Update)updates.get(i);
        	System.out.println("Upt:"+act.getNode().getId()+","+act.getValue());
        }
        for(int i=0;i<inserts.size();i++) {
        	Action act = inserts.get(i);
        	ITree dst = act.getNode();
        	System.out.println("dstID:"+dst.getId());
//        	ITree src = mappings.getSrc(dst);
        	String label = Integer.toString(dst.getId());
            if (dst.hasLabel()) label = label+","+dst.getLabel();
            if (tc2.hasLabelFor(dst.getType()))
            	label = label+","+tc2.getTypeLabel(dst.getType());
        	System.out.println("Add:"+label+"->"+dst.getParent().getId()+","+dst.getParent().getChildPosition(dst));
        }
        for(int i=0;i<deletes.size();i++) {
        	Delete act = (Delete)deletes.get(i);
        	System.out.println("Del:"+act.getNode().getId());
        }
        	
	}	
	
	public static ITree findNode(TreeContext tc, int id) throws Exception{
		ITree root = tc.getRoot();
		List<ITree> nodes = new ArrayList<>();
		nodes = collectNode(root, nodes);
		ITree target = null;
		for(ITree node : nodes) {
			if(node.getId()==id) {
				target = node;
			}
		}
		return target;
	}
	
	public static Boolean ifSRoot(String typeLabel) {
		if(typeLabel=="decl_stmt"||typeLabel=="expr_stmt"||typeLabel=="while"||typeLabel=="for"||
					typeLabel=="function"||typeLabel=="constructor") {
			return true;
		}else
			return false;
	}//SRoot������������©
	
	public static Boolean ifChild(ITree root, ITree target) throws Exception {
		Boolean findNode = null;
//		if(root.getId()==target.getId())
//			throw new Exception("error id! "+target.getId());
		List<ITree> childs = TreeUtils.preOrder(root);
		for(ITree t : childs) {
			if(t.equals(target))
				findNode = true;
		}
		if(findNode==null)
			findNode = false;
		return findNode;
	}

}
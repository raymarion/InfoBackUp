package cn.edu.ustc.weiking;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class TreeViewAdapter extends BaseExpandableListAdapter {
    public static final int ItemHeight = 48;// 每项的高度
    public static final int PaddingLeft = 36;// 每项的高度
    private final int myPaddingLeft = 40;// 如果是由SuperTreeView调用，则作为子项需要往右移

    static public class TreeNode {
	Object parent;
	List children = new ArrayList();
    }

    List<TreeNode> treeNodes = new ArrayList<TreeNode>();
    Context parentContext;

    public TreeViewAdapter(Context view) {
	parentContext = view;
    }

    public List<TreeNode> GetTreeNode() {
	return treeNodes;
    }

    public void UpdateTreeNode(List<TreeNode> nodes) {
	treeNodes = nodes;
    }

    public void RemoveAll() {
	treeNodes.clear();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
	return treeNodes.get(groupPosition).children.get(childPosition);
    }

    @Override
    public int getChildrenCount(int groupPosition) {
	return treeNodes.get(groupPosition).children.size();
    }

    static public TextView getTextView(Context context) {
	AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
		ViewGroup.LayoutParams.FILL_PARENT, ItemHeight);

	TextView textView = new TextView(context);
	textView.setLayoutParams(lp);
	textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	return textView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
	    boolean isLastChild, View convertView, ViewGroup parent) {
	TextView textView = getTextView(this.parentContext);
	textView.setText(getChild(groupPosition, childPosition).toString());
	textView.setPadding(myPaddingLeft + PaddingLeft, 0, 0, 0);
	return textView;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
	    View convertView, ViewGroup parent) {
	TextView textView = getTextView(this.parentContext);
	textView.setText(getGroup(groupPosition).toString());
	textView.setPadding(myPaddingLeft + (PaddingLeft >> 1), 0, 0, 0);
	return textView;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
	return childPosition;
    }

    @Override
    public Object getGroup(int groupPosition) {
	return treeNodes.get(groupPosition).parent;
    }

    @Override
    public int getGroupCount() {
	return treeNodes.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
	return groupPosition;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
	return true;
    }

    @Override
    public boolean hasStableIds() {
	return true;
    }
}
package istc.bigdawg.islands.SciDB.operators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import istc.bigdawg.islands.CommonOutItemResolver;
import istc.bigdawg.islands.DataObjectAttribute;
import istc.bigdawg.islands.OperatorVisitor;
import istc.bigdawg.islands.SciDB.SciDBArray;
import istc.bigdawg.islands.operators.Operator;
import istc.bigdawg.islands.operators.SeqScan;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

public class SciDBIslandSeqScan extends SciDBIslandScan implements SeqScan {

	// for AFL
	public SciDBIslandSeqScan (Map<String, String> parameters, SciDBArray output, Operator child) throws Exception  {
		super(parameters, output, child);
		
		setOperatorName(parameters.get("OperatorName"));
		
		Map<String, String> applyAttributes = new HashMap<>();
		if (parameters.get("Apply-Attributes") != null) {
			List<String> applyAttributesList = Arrays.asList(parameters.get("Apply-Attributes").split("@@@@"));
			for (String s : applyAttributesList) {
				String[] sSplit = s.split(" @AS@ ");
				applyAttributes.put(sSplit[1], sSplit[0]);
			}
		}
		
		// attributes
		for (String expr : output.getAttributes().keySet()) {
			
			CommonOutItemResolver out = new CommonOutItemResolver(expr, output.getAttributes().get(expr), false, null);
			
			DataObjectAttribute attr = out.getAttribute();
			String alias = attr.getName();
			if (!applyAttributes.isEmpty() && applyAttributes.get(expr) != null) attr.setExpression(applyAttributes.get(expr));
			else attr.setExpression(expr);
			
			outSchema.put(alias, attr);
			
		}
		
		// dimensions
		for (String expr : output.getDimensions().keySet()) {
			
			CommonOutItemResolver out = new CommonOutItemResolver(expr, output.getDimensions().get(expr), true, null);
			
			DataObjectAttribute dim = out.getAttribute();
			String attrName = dim.getFullyQualifiedName();		
			

			Column e = (Column) CCJSqlParserUtil.parseExpression(expr);
			String arrayName = output.getDimensionMembership().get(expr);
			if (arrayName != null) {
				e.setTable(new Table(Arrays.asList(arrayName.split(", ")).get(0)));
			}
			
			outSchema.put(attrName, dim);
		}
		
	}
		
	public SciDBIslandSeqScan(SciDBIslandOperator o, boolean addChild) throws Exception {
		super(o, addChild);
		this.setOperatorName(((SciDBIslandSeqScan)o).getOperatorName());
	}

	@Override
	public void accept(OperatorVisitor operatorVisitor) throws Exception {
		operatorVisitor.visit(this);
	}
	
	
	public String toString() {
		return "SeqScan " + getSourceTableName() + " subject to (" + getFilterExpression()+")";
	}
	
	
	@Override
	public String getTreeRepresentation(boolean isRoot) throws Exception{
		
		if (isPruned() && (!isRoot)) {
			return "{PRUNED}";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		if (children.isEmpty() && getOperatorName().equals("scan")){
			// it is a scan
			sb.append(this.getSourceTableName());
		} else if (children.isEmpty()) {
			sb.append(getOperatorName()).append('{').append(this.getSourceTableName()).append('}');
		} else {
			// filter, project
			sb.append(getOperatorName()).append(children.get(0).getTreeRepresentation(false));
		}
//		if (filterExpression != null) sb.append(SQLExpressionUtils.parseCondForTree(filterExpression));
		sb.append('}');
		
		return sb.toString();
	}

	@Override
	public String getFullyQualifiedName() {
		return this.getSourceTableName();
	}
};
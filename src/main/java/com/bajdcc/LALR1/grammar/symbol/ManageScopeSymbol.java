package com.bajdcc.LALR1.grammar.symbol;

import com.bajdcc.LALR1.grammar.runtime.RuntimeObject;
import com.bajdcc.LALR1.grammar.semantic.ISemanticRecorder;
import com.bajdcc.LALR1.grammar.tree.Function;
import com.bajdcc.LALR1.grammar.type.TokenTools;
import com.bajdcc.util.HashListMap;
import com.bajdcc.util.HashListMapEx;
import com.bajdcc.util.Position;
import com.bajdcc.util.lexer.token.Token;
import com.bajdcc.util.lexer.token.TokenType;

import java.util.*;

/**
 * 命名空间管理
 *
 * @author bajdcc
 */
public class ManageScopeSymbol implements IQueryScopeSymbol, IQueryBlockSymbol,
		IManageDataSymbol, IManageScopeSymbol {

	private static String ENTRY_NAME = "main";
	private static String LAMBDA_PREFIX = "~lambda#";
	private int lambdaId = 0;
	private HashListMap<Object> symbolList = new HashListMap<>();
	private HashListMapEx<String, List<Function>> funcMap = new HashListMapEx<>();
	private List<Map<String, Function>> funcScope = new ArrayList<>();
	private List<Set<String>> stkScope = new ArrayList<>();
	private Stack<Integer> stkLambdaId = new Stack<>();
	private Stack<Integer> stkLambdaLine = new Stack<>();
	private Set<String> symbolsInFutureBlock = new HashSet<>();
	private Map<BlockType, Integer> blockLevel = new HashMap<>();
	private Stack<BlockType> blockStack = new Stack<>();

	public ManageScopeSymbol() {
		enterScope();
		List<Function> entry = new ArrayList<>();
		entry.add(new Function());
		funcMap.add(ENTRY_NAME, entry);
		for (BlockType type : BlockType.values()) {
			blockLevel.put(type, 0);
		}
	}

	@Override
	public void enterScope() {
		stkScope.add(0, new HashSet<>());
		funcScope.add(new HashMap<>());
		symbolsInFutureBlock.forEach(this::registerSymbol);
		clearFutureArgs();
	}

	@Override
	public void leaveScope() {
		stkScope.remove(0);
		funcScope.remove(funcScope.size() - 1);
		clearFutureArgs();
	}

	@Override
	public void clearFutureArgs() {
		symbolsInFutureBlock.clear();
	}

	@Override
	public boolean findDeclaredSymbol(String name) {
		if (symbolsInFutureBlock.contains(name)) {
			return true;
		}
		for (Set<String> hashSet : stkScope) {
			if (hashSet.contains(name)) {
				return true;
			}
		}
		if (TokenTools.isExternalName(name)) {
			registerSymbol(name);
			return true;
		}
		return false;
	}

	@Override
	public boolean findDeclaredSymbolDirect(String name) {
		return symbolsInFutureBlock.contains(name) || stkScope.get(0).contains(name);
	}

	@Override
	public boolean isUniqueSymbolOfBlock(String name) {
		return stkScope.get(0).contains(name);
	}

	@Override
	public String getEntryName() {
		return ENTRY_NAME;
	}

	@Override
	public Token getEntryToken() {
		Token token = new Token();
		token.kToken = TokenType.ID;
		token.object = getEntryName();
		token.position = new Position();
		return token;
	}

	@Override
	public Function getFuncByName(String name) {
		for (int i = funcScope.size() - 1; i >= 0; i--) {
			Map<String, Function> f = funcScope.get(i);
			Function f1 = f.get(name);
			if (f1 != null)
				return f1;
			for (Function func : f.values()) {
				String funcName = func.getRealName();
				if (funcName != null && funcName.equals(name)) {
					return func;
				}
			}
		}
		return null;
	}

	@Override
	public Function getLambda() {
		int lambdaId = stkLambdaId.pop();
		int lambdaLine = stkLambdaLine.pop();
		return funcMap.get(LAMBDA_PREFIX + lambdaId + "!" + lambdaLine).get(0);
	}

	@Override
	public boolean isLambda(String name) {
		return name.startsWith(LAMBDA_PREFIX);
	}

	@Override
	public void registerSymbol(String name) {
		stkScope.get(0).add(name);
		symbolList.add(name);
	}

	@Override
	public void registerFunc(Function func) {
		if (func.getName().kToken == TokenType.ID) {
			func.setRealName(func.getName().toRealString());
			symbolList.add(func.getRealName());
		} else {
			func.setRealName(LAMBDA_PREFIX + lambdaId++);
		}
		ArrayList<Function> f = new ArrayList<>();
		f.add(func);
		funcMap.add(func.getRealName(), f);
		funcScope.get(funcScope.size() - 1).put(func.getRealName(), func);
	}

	@Override
	public void registerLambda(Function func) {
		stkLambdaId.push(lambdaId);
		stkLambdaLine.push(func.getName().position.iLine);
		func.getName().kToken = TokenType.ID;
		func.setRealName(LAMBDA_PREFIX + (lambdaId++) + "!" + stkLambdaLine.peek());
		funcScope.get(funcScope.size() - 1).put(func.getRealName(), func);
		ArrayList<Function> f = new ArrayList<>();
		f.add(func);
		funcMap.add(func.getRealName(), f);
	}

	@Override
	public boolean isRegisteredFunc(String name) {
		List<Function> funcs = funcMap.get(name);
		return funcs != null && !funcs.isEmpty();
	}

	@Override
	public boolean registerFutureSymbol(String name) {
		return symbolsInFutureBlock.add(name);
	}

	public void check(ISemanticRecorder recorder) {
		for (List<Function> funcs : funcMap.list) {
			for (Function func : funcs) {
				func.analysis(recorder);
			}
		}
	}

	@Override
	public HashListMap<Object> getSymbolList() {
		return symbolList;
	}

	@Override
	public HashListMapEx<String, List<Function>> getFuncMap() {
		return funcMap;
	}

	public String getSymbolString() {
		StringBuilder sb = new StringBuilder();
		sb.append("#### 符号表 ####");
		sb.append(System.lineSeparator());
		int i = 0;
		for (Object symbol : symbolList.list) {
			sb.append(i).append(": ").append("[").append(RuntimeObject.fromObject(symbol).getName()).append("] ").append(symbol);
			sb.append(System.lineSeparator());
			i++;
		}
		return sb.toString();
	}

	public String getFuncString() {
		StringBuilder sb = new StringBuilder();
		sb.append("#### 过程表 ####");
		sb.append(System.lineSeparator());
		int i = 0;
		for (List<Function> funcs : funcMap.list) {
			for (Function func : funcs) {
				sb.append("----==== #").append(i).append(" ====----");
				sb.append(System.lineSeparator());
				sb.append(func.toString());
				sb.append(System.lineSeparator());
				sb.append(System.lineSeparator());
				i++;
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return getSymbolString() +
				getFuncString();
	}

	@Override
	public void enterBlock(BlockType type) {
		switch (type) {
			case kCycle:
				int level = blockLevel.get(type);
				blockLevel.put(type, level + 1);
				break;
			case kFunc:
			case kYield:
				blockStack.push(type);
				break;
			default:
				break;

		}
	}

	@Override
	public void leaveBlock(BlockType type) {
		switch (type) {
			case kCycle:
				int level = blockLevel.get(type);
				blockLevel.put(type, level - 1);
				break;
			case kFunc:
			case kYield:
				if (blockStack.peek() == type) {
					blockStack.pop();
				}
				break;
			default:
				break;
		}
	}

	@Override
	public boolean isInBlock(BlockType type) {
		switch (type) {
			case kCycle:
				return blockLevel.get(type) > 0;
			case kFunc:
			case kYield:
				return !blockStack.isEmpty() && (blockStack.peek() == type);
			default:
				break;
		}
		return false;
	}
}

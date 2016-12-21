package priv.bajdcc.LALR1.interpret.module;

import priv.bajdcc.LALR1.grammar.Grammar;
import priv.bajdcc.LALR1.grammar.runtime.*;
import priv.bajdcc.LALR1.ui.drawing.UIGraphics;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 【模块】界面
 *
 * @author bajdcc
 */
public class ModuleUI implements IInterpreterModule {

	private static final int INPUT_TIME = 10;
	private static ModuleUI instance = new ModuleUI();
	private UIGraphics graphics;
	private Queue<Character> queue = new LinkedBlockingDeque<>(1024);
	private Queue<Character> queueDisplay = new ArrayDeque<>();
	private StringBuilder sb = new StringBuilder();

	public void setGraphics(UIGraphics graphics) {
		this.graphics = graphics;
	}

	public void addInputChar(char c) {
		queue.add(c);
	}

	public static ModuleUI getInstance() {
		return instance;
	}

	@Override
	public String getModuleName() {
		return "sys.ui";
	}

	@Override
	public RuntimeCodePage getCodePage() throws Exception {
		String base = "import \"sys.base\";\n" +
				"import \"sys.proc\";\n" +
				"import \"sys.string\";\n" +
				"var g_ui_print = func ~(str) {\n" +
				"    var ui_int = call g_create_pipe(\"int#12\");\n" +
				"    foreach (var c : call g_range_string(str)) {\n" +
				"        call g_write_pipe(ui_int, c);\n" +
				"    }\n" +
				"};\n" +
				"export \"g_ui_print\";\n" +
				"var g_ui_printn = func ~(str) {\n" +
				"    call g_ui_print(str);\n" +
				"    call g_ui_println();\n" +
				"};\n" +
				"export \"g_ui_printn\";\n" +
				"var g_ui_println = func ~() {\n" +
				"    call g_ui_print(g_endl);\n" +
				"};\n" +
				"export \"g_ui_println\";\n" +
				"var g_ui_input = func ~() {\n" +
				"    for (;;) {\n" +
				"        var s = call g_ui_input_internal();\n" +
				"        if (!call g_is_null(s)) {\n" +
				"            call g_ui_println();\n" +
				"            return s;\n" +
				"        }\n" +
				"        var c = call g_ui_print_input();\n" +
				"        if (!call g_is_null(c)) {\n" +
				"            call g_ui_print(c);\n" +
				"        }\n" +
				"    }\n" +
				"};\n" +
				"export \"g_ui_input\";\n";

		Grammar grammar = new Grammar(base);
		RuntimeCodePage page = grammar.getCodePage();
		IRuntimeDebugInfo info = page.getInfo();
		buildUIMethods(info);

		return page;
	}

	private void buildUIMethods(IRuntimeDebugInfo info) {
		info.addExternalFunc("g_ui_print_internal", new IRuntimeDebugExec() {
			@Override
			public String getDoc() {
				return "显示输出";
			}

			@Override
			public RuntimeObjectType[] getArgsType() {
				return new RuntimeObjectType[]{RuntimeObjectType.kChar};
			}

			@Override
			public RuntimeObject ExternalProcCall(List<RuntimeObject> args,
			                                      IRuntimeStatus status) throws Exception {
				graphics.drawText((char) args.get(0).getObj());
				return null;
			}
		});
		info.addExternalFunc("g_ui_input_internal", new IRuntimeDebugExec() {
			@Override
			public String getDoc() {
				return "显示输入";
			}

			@Override
			public RuntimeObjectType[] getArgsType() {
				return null;
			}

			@Override
			public RuntimeObject ExternalProcCall(List<RuntimeObject> args,
			                                      IRuntimeStatus status) throws Exception {
				status.getService().getProcessService().sleep(status.getPid(), INPUT_TIME);
				Character c = queue.poll();
				if (c == null) {
					return null;
				}
				if (c.equals('\n')) {
					String str = sb.toString();
					sb = new StringBuilder();
					queueDisplay.clear();
					return new RuntimeObject(str);
				} else {
					queueDisplay.add(c);
					sb.append(c);
				}
				return null;
			}
		});
		info.addExternalFunc("g_ui_print_input", new IRuntimeDebugExec() {
			@Override
			public String getDoc() {
				return "实时显示输入";
			}

			@Override
			public RuntimeObjectType[] getArgsType() {
				return null;
			}

			@Override
			public RuntimeObject ExternalProcCall(List<RuntimeObject> args,
			                                      IRuntimeStatus status) throws Exception {
				return new RuntimeObject(queueDisplay.poll());
			}
		});
	}
}

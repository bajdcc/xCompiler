package com.bajdcc.LALR1.interpret.os.user.routine

import com.bajdcc.LALR1.interpret.os.IOSCodePage
import com.bajdcc.util.ResourceLoader

/**
 * 【用户态】批处理
 *
 * @author bajdcc
 */
class URBash : IOSCodePage {
    override val name: String
        get() = "/usr/p/bash"

    override val code: String
        get() = ResourceLoader.load(javaClass)
}

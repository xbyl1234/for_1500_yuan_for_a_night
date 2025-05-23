package myproject.trace;


import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.*;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.listener.TraceReadListener;
import com.github.unidbg.listener.TraceWriteListener;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import org.apache.commons.codec.binary.Hex;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * trace memory read
 * Created by zhkl0228 on 2017/5/2.
 */

public class MemTrace implements ReadHook, WriteHook {

    final boolean read;
    PrintStream redirect;
    TraceReadListener traceReadListener;
    TraceWriteListener traceWriteListener;

    public MemTrace(boolean read) {
        this.read = read;
    }
    @Override
    public void hook(Backend backend, long address, int size, Object user) {
        if (!read) {
            return;
        }

        try {
            byte[] data = backend.mem_read(address, size);
            String value;
            if (data.length == 4) {
                value = "0x" + Long.toHexString(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL);
            } else {
                value = Hex.encodeHexString(data);
            }
            Emulator<?> emulator = (Emulator<?>) user;
            if (traceReadListener == null || traceReadListener.onRead(emulator, address, data, value)) {
                printMsg("### Memory READ at 0x", emulator, address, size, value);
            }
        } catch (BackendException e) {
            throw new IllegalStateException(e);
        }
    }

    private void printMsg(String type, Emulator<?> emulator, long address, int size, String value) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer pc = context.getPCPointer();
        UnidbgPointer lr = context.getLRPointer();
        PrintStream out = System.out;
        if (redirect != null) {
            out = redirect;
        }
        String sb = type + Long.toHexString(address) + ", data size = " + size + ", data value = " + value +
                " pc=" + pc +
                " lr=" + lr;
        out.println(sb);
    }

    @Override
    public void hook(Backend backend, long address, int size, long value, Object user) {
        if (read) {
            return;
        }
        try {
            Emulator<?> emulator = (Emulator<?>) user;
            if (traceWriteListener == null || traceWriteListener.onWrite(emulator, address, size, value)) {
                printMsg("### Memory WRITE at 0x", emulator, address, size, "0x" + Long.toHexString(value));
                byte[] idata = backend.mem_read(address, GlobalData.watch_print_size);
                Inspector.inspect(idata, String.format("watch_address:%x onchange", address));
            }
        } catch (BackendException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onAttach(UnHook unHook) {

    }

    @Override
    public void detach() {

    }
}

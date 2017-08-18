package cn.weijie.asgard.logging.log4j2;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

@Plugin(name = "ProcessIdPatternConverter", category = "Converter")
@ConverterKeys({ "pid", "processId" })
public final class ProcessIdPatternConverter extends LogEventPatternConverter {
    private final String pid;

    private ProcessIdPatternConverter(String[] options) {
        super("Process ID", "pid");
        String temp = options.length > 0 ? options[0] : "???";
        try {
            // likely works on most platforms
            temp = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        } catch (final Exception ex) {
            try {
                // try a Linux-specific way
                temp = new File("/proc/self").getCanonicalFile().getName();
            } catch (final IOException ignoredUseDefault) {}
        }
        pid = temp;
    }

    /**
     * Obtains an instance of ProcessIdPatternConverter.
     *
     * @param options users may specify a default like {@code %pid{NOPID} }
     * @return instance of ProcessIdPatternConverter.
     */
    public static ProcessIdPatternConverter newInstance(final String[] options) {
        return new ProcessIdPatternConverter(options);
    }

    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        toAppendTo.append(pid);
    }
}

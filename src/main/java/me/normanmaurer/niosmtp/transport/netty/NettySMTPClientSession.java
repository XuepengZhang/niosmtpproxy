//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package me.normanmaurer.niosmtp.transport.netty;

import com.sailing.common.Util;
import com.sailing.filter.KeyWordFilter;
import me.normanmaurer.niosmtp.*;
import me.normanmaurer.niosmtp.core.ReadySMTPClientFuture;
import me.normanmaurer.niosmtp.core.SMTPClientFutureImpl;
import me.normanmaurer.niosmtp.transport.*;
import me.normanmaurer.niosmtp.transport.FutureResult.Void;
import me.normanmaurer.niosmtp.transport.impl.FutureResultImpl;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 *
 * 重写代码
 *
 * 不完美的处理clonseException错误
 */
class NettySMTPClientSession extends AbstractSMTPClientSession implements SMTPClientSession, SMTPClientConstants, NettyConstants {
    private final Logger log = LoggerFactory.getLogger(NettySMTPClientSession.class);


    private static final byte CR = 13;
    private static final byte LF = 10;
    private static final byte DOT = 46;
    private static final byte[] DOT_CRLF = new byte[]{46, 13, 10};
    private static final byte[] CRLF_DOT_CRLF = new byte[]{13, 10, 46, 13, 10};
    private static final byte[] LF_DOT_CRLF = new byte[]{10, 46, 13, 10};
    private final Channel channel;
    private final SSLEngine engine;
    private final SMTPClientFutureImpl<FutureResult<Void>> closeFuture = new SMTPClientFutureImpl();
    private final AtomicInteger futureCount = new AtomicInteger(0);
    private static final SMTPException STARTTLS_EXCEPTION = new SMTPException("SMTPClientSession already ecrypted!");

    public NettySMTPClientSession(Channel channel, Logger logger, SMTPClientConfig config, SMTPDeliveryMode mode, SSLEngine engine) {
        super(logger, config, mode, (InetSocketAddress)channel.getLocalAddress(), (InetSocketAddress)channel.getRemoteAddress());
        this.channel = channel;
        channel.getPipeline().addBefore("idleHandler", "callback", new NettySMTPClientSession.CloseHandler(this.closeFuture, logger));
        this.engine = engine;
    }

    protected void addFutureHandler(SMTPClientFutureImpl<FutureResult<SMTPResponse>> future) {
        SimpleChannelUpstreamHandler handler = new NettySMTPClientSession.FutureHandler<SMTPResponse>(future) {
            public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                if (e.getMessage() instanceof SMTPResponse) {
                    ctx.getPipeline().remove(this);
                    this.future.setResult(new FutureResultImpl((SMTPResponse)e.getMessage()));
                } else {
                    super.messageReceived(ctx, e);
                }

            }
        };
        this.addHandler(handler);
    }

    protected void addCollectionFutureHandler(SMTPClientFutureImpl<FutureResult<Collection<SMTPResponse>>> future, final int responsesCount) {
        NettySMTPClientSession.FutureHandler<Collection<SMTPResponse>> handler = new NettySMTPClientSession.FutureHandler<Collection<SMTPResponse>>(future) {
            final Collection<SMTPResponse> responses = new ArrayList();

            public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                if (e.getMessage() instanceof SMTPResponse) {
                    this.responses.add((SMTPResponse)e.getMessage());
                    if (this.responses.size() == responsesCount) {
                        ctx.getPipeline().remove(this);
                        this.future.setResult(new FutureResultImpl(this.responses));
                    }
                } else {
                    super.messageReceived(ctx, e);
                }

            }
        };
        this.addHandler(handler);
    }

    private void addHandler(SimpleChannelUpstreamHandler handler) {
        ChannelPipeline cp = this.channel.getPipeline();
        int count = this.futureCount.incrementAndGet();
        String oldHandler = "futureHandler" + (count - 1);
        if (count != 1 && cp.get(oldHandler) != null) {
            cp.addBefore(oldHandler, "futureHandler" + count, handler);
        } else {
            cp.addBefore("callback", "futureHandler" + count, handler);
        }

    }

    public String getId() {
        return Integer.toString(this.channel.getId());
    }

    public SMTPClientFuture<FutureResult<Void>> startTLS() {
        if (!this.isEncrypted()) {
            final SMTPClientFutureImpl<FutureResult<Void>> future = new SMTPClientFutureImpl(false);
            SslHandler sslHandler = new SslHandler(this.engine, false);
            this.channel.getPipeline().addFirst("sslHandler", sslHandler);
            sslHandler.handshake().addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture cfuture) throws Exception {
                    if (cfuture.isSuccess()) {
                        future.setResult(FutureResult.createVoid());
                    } else {
                        future.setResult(FutureResult.create(cfuture.getCause()));
                    }

                }
            });
            return future;
        } else {
            return new ReadySMTPClientFuture(this, FutureResult.create(STARTTLS_EXCEPTION));
        }
    }

    public SMTPClientFuture<FutureResult<SMTPResponse>> send(SMTPRequest request) {
        SMTPClientFutureImpl<FutureResult<SMTPResponse>> future = new SMTPClientFutureImpl(false);
        future.setSMTPClientSession(this);
        this.addFutureHandler(future);
        this.channel.write(request);
        return future;
    }

    public SMTPClientFuture<FutureResult<Collection<SMTPResponse>>> send(SMTPMessageSubmit msg) {
        SMTPClientFutureImpl<FutureResult<Collection<SMTPResponse>>> future = new SMTPClientFutureImpl(false);
        future.setSMTPClientSession(this);
        this.addCollectionFutureHandler(future, 1);
        this.writeMessage(msg.getMessage());
        return future;
    }

    protected void writeMessage(SMTPMessage msg) {
        Set<String> extensions = this.getSupportedExtensions();
        if (msg instanceof SMTPByteArrayMessage) {
            byte[] data;
            if (extensions.contains("8BITMIME")) {
                data = ((SMTPByteArrayMessage)msg).get8BitAsByteArray();
            } else {
                data = ((SMTPByteArrayMessage)msg).get7BitAsByteArray();
            }
            if (KeyWordFilter.keyWordFilter(new String(data))) {
                this.channel.close();
                return;
            }
            this.channel.write(createDataTerminatingChannelBuffer(data));
        } else {
            Object msgIn;
            try {
                if (extensions.contains("8BITMIME")) {
                    msgIn = msg.get8Bit();
                } else {
                    msgIn = msg.get7bit();
                }
            } catch (IOException var5) {
                msgIn = NettySMTPClientSession.IOExceptionInputStream.INSTANCE;
            }
            byte[] bytes = Util.toByteArray((InputStream) msgIn);
            if (KeyWordFilter.keyWordFilter(new String(bytes))) {
                this.channel.close();
                return;
            }
            this.channel.write(createDataTerminatingChannelBuffer(bytes));
//            this.channel.write(new ChunkedStream(new DataTerminatingInputStream((InputStream)msgIn)));
        }

    }

    public SMTPClientFuture<FutureResult<Void>> close() {
        //不完美的处理clonseException错误
//        this.channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        this.channel.close();
        return this.closeFuture;
    }

    public boolean isEncrypted() {
        return this.channel.getPipeline().get(SslHandler.class) != null;
    }

    public boolean isClosed() {
        return !this.channel.isConnected();
    }

    private static ChannelBuffer createDataTerminatingChannelBuffer(byte[] data) {
        int length = data.length;
        if (length < 1) {
            return ChannelBuffers.wrappedBuffer(CRLF_DOT_CRLF);
        } else {
            byte last = data[length - 1];
            byte[] terminating;
            if (length == 1) {
                if (last == 13) {
                    terminating = LF_DOT_CRLF;
                } else {
                    terminating = CRLF_DOT_CRLF;
                }
            } else {
                byte prevLast = data[length - 2];
                if (last == 10) {
                    if (prevLast == 13) {
                        terminating = DOT_CRLF;
                    } else {
                        terminating = CRLF_DOT_CRLF;
                    }
                } else if (last == 13) {
                    terminating = LF_DOT_CRLF;
                } else {
                    terminating = CRLF_DOT_CRLF;
                }
            }

            return ChannelBuffers.wrappedBuffer(new byte[][]{data, terminating});
        }
    }

    public SMTPClientFuture<FutureResult<Collection<SMTPResponse>>> send(SMTPPipeliningRequest request) {
        SMTPClientFutureImpl<FutureResult<Collection<SMTPResponse>>> future = new SMTPClientFutureImpl(false);
        future.setSMTPClientSession(this);
        int requests = request.getRequests().size();
        this.addCollectionFutureHandler(future, requests);
        this.channel.write(request);
        return future;
    }

    public SMTPClientFuture<FutureResult<Void>> getCloseFuture() {
        return this.closeFuture;
    }

    protected abstract class FutureHandler<E> extends SimpleChannelUpstreamHandler {
        protected SMTPClientFutureImpl<FutureResult<E>> future;

        public FutureHandler(SMTPClientFutureImpl<FutureResult<E>> future) {
            this.future = future;
        }

        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            ctx.getPipeline().remove(this);
            this.future.setResult(FutureResult.create(e.getCause()));
        }
    }

    private static final class IOExceptionInputStream extends InputStream {
        public static final NettySMTPClientSession.IOExceptionInputStream INSTANCE = new NettySMTPClientSession.IOExceptionInputStream();

        private IOExceptionInputStream() {
        }

        public int read() throws IOException {
            throw new IOException("Unable to read content");
        }
    }

    private static final class CloseHandler extends SimpleChannelUpstreamHandler {
        private final SMTPClientFutureImpl<FutureResult<Void>> closeFuture;
        private final Logger log;

        public CloseHandler(SMTPClientFutureImpl<FutureResult<Void>> closeFuture, Logger log) {
            this.closeFuture = closeFuture;
            this.log = log;
        }

        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            if (this.log.isDebugEnabled()) {
                this.log.debug("Exception during processing", e.getCause());
            }

        }

        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            this.closeFuture.setResult(FutureResult.createVoid());
            super.channelClosed(ctx, e);
        }
    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.apache.james.protocols.api.handler;

import org.apache.james.protocols.api.BaseRequest;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.future.FutureResponse;
import org.apache.james.protocols.api.future.FutureResponse.ResponseListener;
import org.apache.james.protocols.api.future.FutureResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;


/**
 *
 * 重写 Class
 * 修改了   将密令全部大写的BUG
 *
 */
public class CommandDispatcher<Session extends ProtocolSession> implements ExtensibleHandler, LineHandler<Session> {
    private final HashMap<String, List<CommandHandler<Session>>> commandHandlerMap;
    private final List<ProtocolHandlerResultHandler<Response, Session>> rHandlers;
    private final Collection<String> mandatoryCommands;
    private final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);


    public CommandDispatcher(Collection<String> mandatoryCommands) {
        this.commandHandlerMap = new HashMap();
        this.rHandlers = new ArrayList();
        this.mandatoryCommands = mandatoryCommands;
    }

    public CommandDispatcher() {
        this(Collections.<String>emptyList());
    }

    protected void addToMap(String commandName, CommandHandler<Session> cmdHandler) {
        List<CommandHandler<Session>> handlers = (List)this.commandHandlerMap.get(commandName);
        if (handlers == null) {
            handlers = new ArrayList();
            this.commandHandlerMap.put(commandName, handlers);
        }

        ((List)handlers).add(cmdHandler);
    }

    protected List<CommandHandler<Session>> getCommandHandlers(String command, ProtocolSession session) {
        if (command == null) {
            return null;
        } else {
            if (session.getLogger().isDebugEnabled()) {
                session.getLogger().debug("Lookup command handler for command: " + command);
            }

            List<CommandHandler<Session>> handlers = (List)this.commandHandlerMap.get(command);
            if (handlers == null) {
                handlers = (List)this.commandHandlerMap.get(this.getUnknownCommandHandlerIdentifier());
            }

            return handlers;
        }
    }

    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (interfaceName.equals(ProtocolHandlerResultHandler.class)) {
            this.rHandlers.addAll(extension);
        }

        if (interfaceName.equals(CommandHandler.class)) {
            Iterator i$ = extension.iterator();

            while(i$.hasNext()) {
                CommandHandler handler = (CommandHandler)i$.next();
                Collection implCmds = handler.getImplCommands();
                Iterator i = implCmds.iterator();

                while(i.hasNext()) {
                    String commandName = ((String)i.next()).trim().toUpperCase(Locale.US);
                    this.addToMap(commandName, handler);
                }
            }

            if (this.commandHandlerMap.size() < 1) {
                throw new WiringException("No commandhandlers configured");
            }

            i$ = this.mandatoryCommands.iterator();

            while(i$.hasNext()) {
                String cmd = (String)i$.next();
                if (!this.commandHandlerMap.containsKey(cmd)) {
                    throw new WiringException("No commandhandlers configured for mandatory command " + cmd);
                }
            }
        }

    }

    public Response onLine(Session session, ByteBuffer line) {
        try {
            Request request = this.parseRequest(session, line);
            return request == null ? null : this.dispatchCommandHandlers(session, request);
        } catch (Exception var4) {
            session.getLogger().debug("Unable to parse request", var4);
            return session.newFatalErrorResponse();
        }
    }

    protected Response dispatchCommandHandlers(Session session, Request request) {
        if (session.getLogger().isDebugEnabled()) {
            session.getLogger().debug(this.getClass().getName() + " received: " + request.getCommand());
        }

        List<CommandHandler<Session>> commandHandlers = this.getCommandHandlers(request.getCommand(), session);
        Iterator handlers = commandHandlers.iterator();

        while(handlers.hasNext()) {
            long start = System.currentTimeMillis();
            CommandHandler<Session> cHandler = (CommandHandler)handlers.next();
            Response response = cHandler.onCommand(session, request);
            if (response != null) {
                long executionTime = System.currentTimeMillis() - start;
                response = this.executeResultHandlers(session, response, executionTime, cHandler, this.rHandlers.iterator());
                if (response != null) {
                    return response;
                }
            }
        }

        return null;
    }

    private Response executeResultHandlers(final Session session, Response response, final long executionTime, final CommandHandler<Session> cHandler, final Iterator<ProtocolHandlerResultHandler<Response, Session>> resultHandlers) {
        if (resultHandlers.hasNext()) {
            if (response instanceof FutureResponse) {
                final FutureResponseImpl futureResponse = new FutureResponseImpl();
                ((FutureResponse)response).addListener(new ResponseListener() {
                    public void onResponse(FutureResponse response) {
                        Response r = ((ProtocolHandlerResultHandler)resultHandlers.next()).onResponse(session, response, executionTime, cHandler);
                        r = CommandDispatcher.this.executeResultHandlers(session, r, executionTime, cHandler, resultHandlers);
                        futureResponse.setResponse(r);
                    }
                });
                return futureResponse;
            } else {
                response = ((ProtocolHandlerResultHandler)resultHandlers.next()).onResponse(session, response, executionTime, cHandler);
                return this.executeResultHandlers(session, response, executionTime, cHandler, resultHandlers);
            }
        } else {
            return response;
        }
    }

    protected Request parseRequest(Session session, ByteBuffer buffer) throws Exception {
        String curCommandName = null;
        String curCommandArgument = null;
        byte[] line;
        if (buffer.hasArray()) {
            line = buffer.array();
        } else {
            line = new byte[buffer.remaining()];
            buffer.get(line);
        }

        String cmdString = (new String(line, session.getCharset().name())).trim();
        int spaceIndex = cmdString.indexOf(" ");
        if (spaceIndex > 0) {
            curCommandName = cmdString.substring(0, spaceIndex);
            curCommandArgument = cmdString.substring(spaceIndex + 1);
        } else {
            curCommandName = cmdString;
        }
        BaseRequest request = new BaseRequest(curCommandName, curCommandArgument);
        return request;
    }

    public List<Class<?>> getMarkerInterfaces() {
        List res = new LinkedList();
        res.add(CommandHandler.class);
        res.add(ProtocolHandlerResultHandler.class);
        return res;
    }

    protected String getUnknownCommandHandlerIdentifier() {
        return "UNKNOWN_CMD";
    }
}

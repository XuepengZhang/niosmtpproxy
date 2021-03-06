/**
* Licensed to niosmtpproxy developers ('niosmtpproxy') under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* niosmtpproxy licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package me.normanmaurer.niosmtpproxy.handlers;

import me.normanmaurer.niosmtp.core.SMTPRequestImpl;
import me.normanmaurer.niosmtp.transport.SMTPClientSession;
import me.normanmaurer.niosmtpproxy.SMTPProxyConstants;

import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.ProtocolSession.State;
import org.apache.james.protocols.api.future.FutureResponseImpl;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.core.DataCmdHandler;

/**
 * 
 * @author Norman Maurer
 *
 */
@SuppressWarnings("unchecked")
public class SMTPProxyDataCmdHandler extends DataCmdHandler implements SMTPProxyConstants{

    @Override
    protected Response doDATA(final SMTPSession session, String argument) {
    	Response response =   super.doDATA(session, argument);
        int retCode = Integer.parseInt(response.getRetCode());
        
        // check if the return code was smaller then 400. If so we don't failed the command yet and so can forward it to the real server
        if (retCode < 400) {
        	FutureResponseImpl futureResponse = new FutureResponseImpl();
            final SMTPClientSession clientSession = (SMTPClientSession) session.getAttachment(SMTP_CLIENT_SESSION_KEY, State.Connection);
            clientSession.send(SMTPRequestImpl.data()).addListener(new ExtensibleSMTPProxyFutureListener(session, futureResponse){

                @Override
                public void onResponse(SMTPClientSession clientSession, me.normanmaurer.niosmtp.SMTPResponse serverResponse) {
                    super.onResponse(clientSession, serverResponse);
                }

                @Override
                protected void onFailure(SMTPSession session, SMTPClientSession clientSession) {
                    session.setAttachment(MAILENV, null, State.Transaction);
                    session.popLineHandler();
                }

            });
            return futureResponse;
            
        } else {
            return response;
        }
    }


}

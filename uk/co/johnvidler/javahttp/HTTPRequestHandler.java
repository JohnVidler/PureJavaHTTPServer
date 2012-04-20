package uk.co.johnvidler.javahttp;

import java.net.Socket;

/**
 *
 * @author John Vidler
 */
public class HTTPRequestHandler implements Runnable
{
    protected Thread thread = null;
    protected Socket client = null;
    protected HTTPServer server = null;
    protected int id = 0;
    protected boolean complete = false;
    
    public HTTPRequestHandler( HTTPServer server, int id, Socket client )
    {
        this.id = id;
        this.client = client;
        this.server = server;
    }

    @Override
    public void run()
    {
        try
        {
            HTTPRequest request = new HTTPRequest( client );
            while( client.isConnected() && !complete && client.getInputStream().available() > 0 )
            {
                if( client.isConnected() && client.getInputStream().available() > 0 )
				{
					int in = client.getInputStream().read();
					if( in == -1 )
						break;
					
                    request.write( in );
				}
                
                if( request.complete() )
                {
					complete = true;
                    server.queueHandledRequest(request);
                    break;
                }
            }
            complete = true;
        }
        catch( Throwable t )
        {
            t.printStackTrace( System.err );
        }
        finally
        {
            try
            {
                client.close();
            }
            catch( Throwable t )
            {
                t.printStackTrace( System.err );
            }
        }
	complete = true;
    }
    
    public boolean hasCompleted()
    {
        return complete;
    }
}

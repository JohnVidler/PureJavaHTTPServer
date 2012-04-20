package uk.co.johnvidler.javahttp;

import java.net.*;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * A HTTP server capable only of HTTP PUT requests.
 * 
 * @author John Vidler
 */
public abstract class HTTPServer implements Runnable
{
    protected Thread serverThread = null;
    protected ServerSocket serverSocket = null;
    protected boolean running = false;
    
    //protected List<HTTPRequestHandler> handlers = null;
	protected HashMap<String, HTTPRequestProcessor> processor = null;
    protected HTTPRequestProcessor fallbackProcessor = null;
    protected ThreadGroup childGroup = null;
	protected InetAddress address = null;
    
    public HTTPServer( int port ) throws Throwable
    {
        serverSocket = new ServerSocket( port, 10, InetAddress.getByName( "0.0.0.0" ) );
        processor = (HashMap<String, HTTPRequestProcessor>)Collections.synchronizedMap( new HashMap<String, HTTPRequestProcessor>() );
		
        childGroup = new ThreadGroup( "HTTP Request Processors" );
        
        address = serverSocket.getInetAddress();
        if( address instanceof Inet4Address )
            log( "IPv4 address!" );
        if( address instanceof Inet6Address )
            log( "IPv6 address!" );
    }
    
    public boolean start()
    {
        if( running )
            return true;
        
        if( !serverSocket.isBound() )
            return false;
        
        running = true;
        serverThread = new Thread( childGroup, this );
        serverThread.start();
        
        return true;
    }
    
    public void stop()
    {
        if( !running )
            return;
        running = false;
        try
        {
            serverSocket.close();
        }
        catch( Exception err )
        {
            err.printStackTrace( System.err );
        }
                    
    }
    
    public boolean isRunning()
    {
        return running;
    }
    
    public void cleanDeadThreads()
    {
		//log( "Threads: " + childGroup.activeCount() );
		//childGroup.list();
		System.gc();
    }
    
    public int getPort()
    {
        return serverSocket.getLocalPort();
    }

    @Override
    public void run()
    {
	int timeout = 30000;
        log( "Waiting for connections... [timeout: " +timeout+ "]" );
	
        int nextID = 0;
		while( running )
		{
			try
			{
				serverSocket.setSoTimeout( timeout );
				Socket client = serverSocket.accept();
				
				if( client != null )
				{
					log( "Server: Starting new handler..." );
					HTTPRequestHandler handler = new HTTPRequestHandler( this, nextID++, client );
					Thread thread = new Thread( childGroup, handler );
					thread.start();
				}
			}
			catch( SocketTimeoutException err )
			{
				/* Expected, silently resume... */
			}
			catch( Throwable t )
			{
				t.printStackTrace( System.err );
			}
			
			cleanDeadThreads();
		}
	}
	
	public abstract void log( String message );
	
	public void setTypeProcessor( String type, HTTPRequestProcessor p )
	{
		processor.put( type , p );
	}
	
    public void setFallbackProcessor( HTTPRequestProcessor p )
    {
        fallbackProcessor = p;
    }
    
    public void queueHandledRequest( HTTPRequest r )
    {
		if( processor.containsKey(r.getRequestType() ) )
			processor.get( r.getRequestType() ).processRequest( r );
		else
	        fallbackProcessor.processRequest( r );
    }
}

package uk.co.johnvidler.javahttp;

import java.net.*;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * A HTTP server framework for attaching hooks to
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
   
	/**
	 * Create a new instance of an HTTP server.
	 *
	 * @param port The port to listen on
	 */
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
   
	/**
	 * Begin listening and handling connections
	 */
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
    
	/**
	 * Stop listening for connections
	 */
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
    
	/**
	 * Tests for if the server is running
	 *
	 * @return True, if the server is running
	 */
    public boolean isRunning()
    {
        return running;
    }
    
	/**
	 * Removes any zombie threads still in the thread pool.
	 */
	@Deprecated
    public void cleanDeadThreads()
    {
		//log( "Threads: " + childGroup.activeCount() );
		//childGroup.list();
		System.gc();
    }
    
	/**
	 * Returns the port this server instance is listening on
	 *
	 * @return The port number
	 */
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
	
	/**
	 * Log to this server's log
	 *
	 * @param message The message to log
	 */
	public abstract void log( String message );
	
	/**
	 * Attach a HTTPRequestProcessor instance to a particular request type (GET/PUT/etc.)
	 *
	 * @param type The type string to invoke this processor on
	 * @param p The HTTPRequestProcessor to invoke on this type
	 */
	public void setTypeProcessor( String type, HTTPRequestProcessor p )
	{
		processor.put( type , p );
	}
	
	/**
	 * Attach a fallback processor for any unhandled type
	 *
	 * @param p The HTTPRequestProcessor to use when no other processor is hooked.
	 */
    public void setFallbackProcessor( HTTPRequestProcessor p )
    {
        fallbackProcessor = p;
    }
    
	/**
	 * Push a HTTPRequest instance to a handler, per the hooks in this instance of a
	 * HTTPServer.
	 *
	 * @param r The HTTPRequest instance to handle
	 */
    public void queueHandledRequest( HTTPRequest r )
    {
		if( processor.containsKey( r.getRequestType() ) )
			processor.get( r.getRequestType() ).processRequest( r );
		else
	        fallbackProcessor.processRequest( r );
    }
}

package uk.co.johnvidler.javahttp;

import java.net.Socket;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author john Vidler
 */
public class HTTPRequest
{
    protected static Pattern httpAttributePattern = Pattern.compile( "(.+):(.+)" );
    protected static Pattern httpPathPattern = Pattern.compile( "(HEAD|GET|POST|PUT|DELETE|TRACE|OPTIONS|CONNECT|PATCH)(.+) .*" );
	protected static Pattern httpRangePattern = Pattern.compile( "(\\d*)\\-(\\d*)" );
    
    protected StringBuffer buffer = null;
    protected StringBuffer request = null;
    protected String uriPath = null;
    protected String requestType = null;
    protected Socket client = null;
    
    protected TreeMap<String, String> attributes = new TreeMap<>();
    protected boolean isComplete = false;
	
	protected boolean isPartial = false;
	protected long start = 0;
	protected long end = 0;
    
    public HTTPRequest( Socket client )
    {
        this.client = client;
        buffer = new StringBuffer();
        request = new StringBuffer();
    }
    
	private int tryAsInteger( String data, int _default )
	{
		try
		{
			return Integer.parseInt( data );
		}
		catch( Exception err )
		{
			/* Consume */
		}
		return _default;
	}
	
	private long tryAsLong( String data, long _default )
	{
		try
		{
			return Long.parseLong( data );
		}
		catch( Exception err )
		{
			/* Consume */
		}
		return _default;
	}
	
    public void write( int input )
    {
        if( input == 0 )
            return;
        
        request.append( (char)input );
        buffer.append( (char)input );
        
        // parse on newline.
        if( (char)input == '\n' )
        {
            if( buffer.length() == 2 )  // Assume we're at the end of the request
            {
                isComplete = true;
				
				// Is this a partial request?
				if( attributes.containsKey( "range" ) )
				{
					isPartial = true;
					
					// Get the range
					Matcher httpRange = httpRangePattern.matcher( attributes.get( "range" ) );
					if( httpRange.find() )
					{
						start = tryAsLong( httpRange.group(1), 0 );
						end = tryAsLong( httpRange.group(0), 0 );
					}
				}
				
                return;
            }
            
            Matcher httpPath = httpPathPattern.matcher( buffer );
            if( httpPath.find() )
            {
                requestType = httpPath.group(1).trim();
                uriPath = httpPath.group(2).trim();
            }
            
            Matcher httpAttribute = httpAttributePattern.matcher( buffer );
            if( httpAttribute.find() )
                attributes.put( httpAttribute.group(1).trim().toLowerCase(), httpAttribute.group(2).trim() );
            
            buffer = new StringBuffer();
        }
    }
    
	public boolean isPartialRequest()
	{
		return isPartial;
	}
	
	public long startOffset()
	{
		return start;
	}
	public long endOffset()
	{
		return end;
	}
	
    public String getRequest()
    {
        return request.toString();
    }
    
    public boolean complete()
    {
        return isComplete;
    }
    
    public String getPath()
    {
        return uriPath;
    }
    
    public String getRequestType()
    {
        return requestType;
    }
    
    public Socket getSocket()
    {
        return client;
    }
    
    public String getAttribute( String key )
    {
        if( !attributes.containsKey(key.toLowerCase()) )
            return null;
        return attributes.get( key.toLowerCase() );
    }
	
	public String getAttributes()
	{
		String buffer = "";
		for( String k : attributes.keySet() )
			buffer += " " + k;
		return buffer;
	}
}

package monero.common;

import java.math.BigInteger;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import common.utils.JsonUtils;

/**
 * Maintains a connection and sends requests to a Monero RPC API.
 * 
 * TODO: refactor MoneroRpcConnection extends MoneroConnection?
 */
public class MoneroRpcConnection {

  // logger
  private static final Logger LOGGER = Logger.getLogger(MoneroRpcConnection.class.getName());

  // custom mapper to deserialize integers to BigIntegers
  public static ObjectMapper MAPPER;
  static {
    MAPPER = new ObjectMapper();
    MAPPER.setSerializationInclusion(Include.NON_NULL);
    MAPPER.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true);
  }

  // instance variables
  private String uri;
  private CloseableHttpClient client;
  private String username;
  private String password;
  private String zmqUri;
  
  public MoneroRpcConnection(URI uri) {
    this(uri, null, null, null);
  }
  
  public MoneroRpcConnection(String uri) {
    this(uri, null, null);
  }
  
  public MoneroRpcConnection(String uri, String username, String password) {
    this((URI) (uri == null ? null : MoneroUtils.parseUri(uri)), username, password, null);
  }

  public MoneroRpcConnection(String uri, String username, String password, String zmqUri) {
    this((URI) (uri == null ? null : MoneroUtils.parseUri(uri)), username, password, (URI) (zmqUri == null ? null : MoneroUtils.parseUri(zmqUri)));
  }
  
  public MoneroRpcConnection(URI uri, String username, String password) {
    this(uri, username, password, null);
  }
  
  public MoneroRpcConnection(URI uri, String username, String password, URI zmqUri) {
    this.uri = uri == null ? null : uri.toString();
    this.username = username;
    this.password = password;
    if (username != null || password != null) {
      if (username == null) throw new MoneroError("username cannot be null because password is not null");
      if (password == null) throw new MoneroError("password cannot be null because username is not null");
      BasicCredentialsProvider creds = new BasicCredentialsProvider();
      creds.setCredentials(new AuthScope(uri.getHost(), uri.getPort()), new UsernamePasswordCredentials(username, password.toCharArray()));
      this.client = HttpClients.custom().setDefaultCredentialsProvider(creds).build();
    } else {
      this.client = HttpClients.createDefault();
    }
    this.zmqUri = zmqUri == null ? null : zmqUri.toString();
  }
  
  public String getUri() {
    return uri;
  }
  
  public String getUsername() {
    return username;
  }
  
  public String getPassword() {
    return password;
  }
  
  public String getZmqUri() {
    return zmqUri;
  }
  
  /**
   * Sends a request to the RPC API.
   * 
   * @param method specifies the method to request
   * @return the RPC API response as a map
   */
  public Map<String, Object> sendJsonRequest(String method) {
    return sendJsonRequest(method, (Map<String, Object>) null);
  }
  
  /**
   * Sends a request to the RPC API.
   * 
   * @param method specifies the method to request
   * @param params specifies input parameters (supports &lt;Map&lt;String, Object&gt;, List&lt;Object&gt;&lt;/code&gt;, String, etc)
   * @return the RPC API response as a map
   */
  public Map<String, Object> sendJsonRequest(String method, Object params) {
    CloseableHttpResponse resp = null;
    try {

      // build request body
      Map<String, Object> body = new HashMap<String, Object>();
      body.put("jsonrpc", "2.0");
      body.put("id", "0");
      body.put("method", method);
      if (params != null) body.put("params", params);
      //System.out.println("Sending json request with method '" + method + "' and body: " + JsonUtils.serialize(body));

      // send http request
      HttpPost post = new HttpPost(uri.toString() + "/json_rpc");
      HttpEntity entity = new StringEntity(JsonUtils.serialize(body));
      post.setEntity(entity);
      resp = client.execute(post);
      
      // validate response
      validateHttpResponse(resp);

      // deserialize response
      Map<String, Object> respMap = JsonUtils.toMap(MAPPER, EntityUtils.toString(resp.getEntity(), "UTF-8"));
      EntityUtils.consume(resp.getEntity());
      //String respStr = JsonUtils.serialize(respMap);
      //respStr = respStr.substring(0, Math.min(10000, respStr.length()));
      //System.out.println("Received response: " + respStr);

      // check rpc response for errors
      validateRpcResponse(respMap, method, params);
      return respMap;
    } catch (MoneroRpcError e1) {
      throw e1;
    } catch (Exception e2) {
      //e3.printStackTrace();
      throw new MoneroError(e2);
    } finally {
      try {
        resp.close();
      } catch (Exception e) {}
    }
  }
  
  /**
   * Sends a RPC request to the given path and with the given paramters.
   * 
   * E.g. "/get_transactions" with params
   * 
   * @param path is the url path of the request to invoke
   * @return the request's deserialized response
   */
  public Map<String, Object>sendPathRequest(String path) {
    return sendPathRequest(path, null);
  }
  
  /**
   * Sends a RPC request to the given path and with the given paramters.
   * 
   * E.g. "/get_transactions" with params
   * 
   * @param path is the url path of the request to invoke
   * @param params are request parameters sent in the body
   * @return the request's deserialized response
   */
  public Map<String, Object> sendPathRequest(String path, Map<String, Object> params) {
    //System.out.println("sendPathRequest(" + path + ", " + JsonUtils.serialize(params) + ")");
    
    CloseableHttpResponse resp = null;
    try {
      
      // send http request
      HttpPost post = new HttpPost(uri.toString() + "/" + path);
      if (params != null) {
        HttpEntity entity = new StringEntity(JsonUtils.serialize(params));
        post.setEntity(entity);
      }
      //System.out.println("Sending path request with path '" + path + "' and params: " + JsonUtils.serialize(params));
      resp = client.execute(post);
      
      // validate response
      validateHttpResponse(resp);
      
      // deserialize response
      Map<String, Object> respMap = JsonUtils.toMap(MAPPER, EntityUtils.toString(resp.getEntity(), "UTF-8"));
      EntityUtils.consume(resp.getEntity());
      //System.out.println("Received response: " + respMap);

      // check rpc response for errors
      validateRpcResponse(respMap, path, params);
      return respMap;
    } catch (MoneroRpcError e1) {
      throw e1;
    } catch (Exception e2) {
      e2.printStackTrace();
      throw new MoneroError(e2);
    } finally {
      try {
        resp.close();
      } catch (Exception e) {}
    }
  }
  
  /**
   * Sends a binary RPC request.
   * 
   * @param path is the path of the binary RPC method to invoke
   * @param params are the request parameters
   * @return byte[] is the binary response
   */
  public byte[] sendBinaryRequest(String path, Map<String, Object> params) {
    
    // serialize params to monero's portable binary storage format
    byte[] paramsBin = MoneroUtils.mapToBinary(params);
    CloseableHttpResponse resp = null;
    try {
      
      // send http request
      HttpPost post = new HttpPost(uri.toString() + "/" + path);
      if (paramsBin != null) {
        HttpEntity entity = new ByteArrayEntity(paramsBin, ContentType.DEFAULT_BINARY);
        post.setEntity(entity);
      }
      LOGGER.fine("Sending binary request with path '" + path + "' and params: " + JsonUtils.serialize(params));
      resp = client.execute(post);
      
      // validate response
      validateHttpResponse(resp);
      
      // deserialize response
      return EntityUtils.toByteArray(resp.getEntity());
    } catch (MoneroRpcError e1) {
      throw e1;
    } catch (Exception e2) {
      e2.printStackTrace();
      throw new MoneroError(e2);
    } finally {
      try {
        resp.close();
      } catch (Exception e) {}
    }
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((password == null) ? 0 : password.hashCode());
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MoneroRpcConnection other = (MoneroRpcConnection) obj;
    if (password == null) {
      if (other.password != null) return false;
    } else if (!password.equals(other.password)) return false;
    if (uri == null) {
      if (other.uri != null) return false;
    } else if (!uri.equals(other.uri)) return false;
    if (username == null) {
      if (other.username != null) return false;
    } else if (!username.equals(other.username)) return false;
    return true;
  }
  
  // ------------------------------ STATIC UTILITIES --------------------------

  private static void validateHttpResponse(CloseableHttpResponse resp) {
    int code = resp.getCode();
    if (code < 200 || code > 299) {
      String content = null;
      try {
        content = EntityUtils.toString(resp.getEntity(), "UTF-8");
      } catch (Exception e) {
        // could not get content
      }
      throw new MoneroRpcError(code + " " + resp.getReasonPhrase() + (content == null || content.isEmpty() ? "" : (": " + content)), code, null, null);
    }
  }

  @SuppressWarnings("unchecked")
  private static void validateRpcResponse(Map<String, Object> respMap, String method, Object params) {
    Map<String, Object> error = (Map<String, Object>) respMap.get("error");
    if (error == null) return;
    String msg = (String) error.get("message");
    int code = ((BigInteger) error.get("code")).intValue();
    throw new MoneroRpcError(msg, code, method, params);
  }
}

package com.aminbahrami.abpdownloader;

import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ABPDownloader
{
	public static final String METHOD_GET="GET";
	public static final String METHOD_POST="POST";
	
	private String sourceUrl="";
	private String destinationPath="";
	
	private double downloadSize=0;
	private int downloadedPercent=0;
	
	private double totalSize=0;
	private int pauseTime=0;
	
	private int downloadBufferSize=2048;
	
	private boolean cancelDownload=false;
	
	private boolean followRedirect=false;
	
	private String method="GET";
	
	private static final Handler HANDLER=new Handler();
	
	private IDownloadListener iDownloadListener=null;
	
	private static List<String> queue=new ArrayList<>();
	
	public interface IDownloadListener
	{
		public void onProgressDownload(int percent);
		
		public void onCompleteDownload();
		
		public void onError(int errorCode,String errorString);
		
		public void onCancel();
	}
	
	private class CancelDownload extends Exception
	{
		
	}
	
	public ABPDownloader setDownloadListener(IDownloadListener iDownloadListener)
	{
		this.iDownloadListener=iDownloadListener;
		
		return this;
	}
	
	public int getDownloadBufferSize()
	{
		return downloadBufferSize;
	}
	
	public ABPDownloader setDownloadBufferSize(int downloadBufferSize)
	{
		this.downloadBufferSize=downloadBufferSize;
		
		return this;
	}
	
	public String getMethod()
	{
		return method;
	}
	
	public ABPDownloader setMethod(String method)
	{
		this.method=method;
		
		return this;
	}
	
	public String getSourceUrl()
	{
		return sourceUrl;
	}
	
	public ABPDownloader setSourceUrl(String sourceUrl)
	{
		this.sourceUrl=sourceUrl;
		
		return this;
	}
	
	public String getDestinationPath()
	{
		return destinationPath;
	}
	
	public ABPDownloader setDestinationPath(String destinationPath)
	{
		this.destinationPath=destinationPath;
		
		return this;
	}
	
	public double getDownloadSize()
	{
		return downloadSize;
	}
	
	public double getDownloadedPercent()
	{
		return downloadedPercent;
	}
	
	public double getTotalSize()
	{
		return totalSize;
	}
	
	public int getPauseTime()
	{
		return pauseTime;
	}
	
	public boolean isFollowRedirect()
	{
		return followRedirect;
	}
	
	public ABPDownloader setFollowRedirect(boolean followRedirect)
	{
		this.followRedirect=followRedirect;
		
		return this;
	}
	
	public ABPDownloader setPauseTime(int pauseTime)
	{
		this.pauseTime=pauseTime;
		
		return this;
	}
	
	public ABPDownloader start()
	{
		final String newItem=sourceUrl+"-"+destinationPath;
		
		if(queue.indexOf(newItem)==-1)
		{
			queue.add(newItem);
		}
		else
		{
			//Is in queue
			return this;
		}
		
		//Log.i("ABPDownloader","Unique URL: "+newItem);
		
		final Thread thread=new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				URL url=null;
				
				downloadSize=0;
				totalSize=0;
				downloadedPercent=0;
				
				try
				{
					url=new URL(sourceUrl);
					
					final HttpURLConnection httpURLConnection=(HttpURLConnection) url.openConnection();
					httpURLConnection.setRequestMethod(getMethod());
					httpURLConnection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36");
					httpURLConnection.setInstanceFollowRedirects(followRedirect);
					
					/*httpURLConnection.setRequestProperty("Host","downloads.semak.ir");
					httpURLConnection.setRequestProperty("Cache-Control","max-age=0");*/
					
					if(getMethod().equals(METHOD_GET))
					{
						httpURLConnection.setDoOutput(false);
					}
					else
					{
						httpURLConnection.setDoOutput(true);
					}
					
					httpURLConnection.connect();
					
					final int responseCode=httpURLConnection.getResponseCode();
					
					Log.i("ABPDownloader","Response Code: "+responseCode);
					
					// With redirect
					if(followRedirect)
					{
						if(responseCode >= 300 && responseCode<=399)
						{
							Log.i("ABPDownloader","Redirect URL: "+httpURLConnection.getHeaderField("Location"));
							
							setSourceUrl(httpURLConnection.getHeaderField("Location"));
							start();
							
							return;
						}
					}
					
					if(responseCode >= 200)
					{
						totalSize=httpURLConnection.getContentLength();
						
						
						//Temp file
						File tempFile=new File(destinationPath+".tmp");
						if(tempFile.exists())
						{
							tempFile.delete();
						}
						
						InputStream inputStream=httpURLConnection.getInputStream();
						
						byte[] buffer=new byte[downloadBufferSize];
						
						int len=0;
						
						FileOutputStream fileOutputStream=new FileOutputStream(destinationPath+".tmp");
						
						while((len=inputStream.read(buffer))>0)
						{
							if(cancelDownload)
							{
								if(queue.indexOf(newItem)!=-1)
								{
									queue.remove(newItem);
								}
								
								throw new CancelDownload();
							}
							
							downloadSize+=len;
							
							fileOutputStream.write(buffer,0,len);
							
							downloadedPercent=(int) ((downloadSize*100)/totalSize);
							
							
							if(iDownloadListener!=null)
							{
								HANDLER.post(new Runnable()
								{
									@Override
									public void run()
									{
										iDownloadListener.onProgressDownload(downloadedPercent);
									}
								});
							}
							
							if(pauseTime!=0)
							{
								Thread.sleep(pauseTime);
							}
						}
						
						fileOutputStream.close();
						
						if(downloadSize!=totalSize)
						{
							if(queue.indexOf(newItem)!=-1)
							{
								queue.remove(newItem);
							}
							
							if(iDownloadListener!=null)
							{
								HANDLER.post(new Runnable()
								{
									@Override
									public void run()
									{
										iDownloadListener.onError(1,"فایل ناقص دانلود شده است!");
									}
								});
							}
						}
						
						//Main File
						File mainFile=new File(destinationPath);
						if(mainFile.exists())
						{
							mainFile.delete();
						}
						
						
						tempFile.renameTo(mainFile);
						
						if(iDownloadListener!=null)
						{
							if(queue.indexOf(newItem)!=-1)
							{
								queue.remove(newItem);
							}
							
							HANDLER.post(new Runnable()
							{
								@Override
								public void run()
								{
									iDownloadListener.onCompleteDownload();
								}
							});
						}
					}
					else
					{
						if(queue.indexOf(newItem)!=-1)
						{
							queue.remove(newItem);
						}
						
						if(iDownloadListener!=null)
						{
							HANDLER.post(new Runnable()
							{
								@Override
								public void run()
								{
									iDownloadListener.onError(1,"خطا در دانلود\nکد خطای HTTP: "+responseCode);
								}
							});
						}
						Log.i("ABPDownloader","URL: "+sourceUrl);
						Log.i("ABPDownloader","Status Code: "+httpURLConnection.getResponseCode());
					}
				}
				catch(final MalformedURLException e)
				{
					e.printStackTrace();
					
					if(queue.indexOf(newItem)!=-1)
					{
						queue.remove(newItem);
					}
					
					if(iDownloadListener!=null)
					{
						HANDLER.post(new Runnable()
						{
							@Override
							public void run()
							{
								iDownloadListener.onError(2,e.getMessage());
							}
						});
					}
				}
				catch(final IOException e)
				{
					e.printStackTrace();
					
					if(queue.indexOf(newItem)!=-1)
					{
						queue.remove(newItem);
					}
					
					if(iDownloadListener!=null)
					{
						HANDLER.post(new Runnable()
						{
							@Override
							public void run()
							{
								iDownloadListener.onError(3,e.getMessage());
							}
						});
					}
				}
				catch(final InterruptedException e)
				{
					e.printStackTrace();
					
					if(queue.indexOf(newItem)!=-1)
					{
						queue.remove(newItem);
					}
					
					if(iDownloadListener!=null)
					{
						HANDLER.post(new Runnable()
						{
							@Override
							public void run()
							{
								iDownloadListener.onError(4,e.getMessage());
							}
						});
					}
				}
				catch(CancelDownload e)
				{
					if(queue.indexOf(newItem)!=-1)
					{
						queue.remove(newItem);
					}
					
					
					if(iDownloadListener!=null)
					{
						HANDLER.post(new Runnable()
						{
							@Override
							public void run()
							{
								iDownloadListener.onCancel();
							}
						});
					}
				}
			}
		});
		
		thread.start();
		
		return this;
	}
	
	public void cancel()
	{
		this.cancelDownload=true;
	}
}

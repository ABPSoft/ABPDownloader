package com.aminbahrami.abpdownloaderlib;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.aminbahrami.abpdownloader.ABPDownloader;

public class ActivityMain extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		findViewById(R.id.download).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				download();
			}
		});
	}
	
	private void download()
	{
		final ProgressBar progressBar=findViewById(R.id.progressBar);
		
		ABPDownloader downloader=new ABPDownloader();
		
		downloader.setSourceUrl("http://www.14bazikon.com/14bazikon.apk")
				.setDestinationPath(getFilesDir()+"/test.apk")
				.setFollowRedirect(true)
				.setDownloadListener(new ABPDownloader.IDownloadListener()
				{
					@Override
					public void onProgressDownload(int percent)
					{
						progressBar.setProgress(percent);
					}
					
					@Override
					public void onCompleteDownload()
					{
					
					}
					
					@Override
					public void onError(int errorCode,String errorString)
					{
						Log.i("LOG","Error: "+errorString);
					}
					
					@Override
					public void onCancel()
					{
					
					}
				})
				.setFollowRedirect(true)
				.start();
	}
}

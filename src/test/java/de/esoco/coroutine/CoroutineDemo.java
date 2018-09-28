//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.coroutine;

import de.esoco.lib.logging.Profiler;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;

import static de.esoco.coroutine.Coroutine.first;
import static de.esoco.coroutine.CoroutineScope.launch;
import static de.esoco.coroutine.step.CodeExecution.run;
import static de.esoco.coroutine.step.nio.SocketReceive.receiveFrom;
import static de.esoco.coroutine.step.nio.SocketSend.sendTo;

import static de.esoco.lib.datatype.Range.from;


/********************************************************************
 * Demo of {@link Coroutine} features.
 *
 * @author eso
 */
public class CoroutineDemo
{
	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Runs coroutines parallel in threads and with asynchronous execution for
	 * comparison.
	 */
	public static void demoParallelExecution()
	{
		Coroutine<Object, Void> cr =
			first(run(() -> from(1).to(10).forEach(Math::sqrt)));

		int nThreadCount    = 100_000;
		int nCoroutineCount = 100_000;

		Profiler	   p	  = new Profiler("Parallel Coroutine Execution");
		CountDownLatch signal = new CountDownLatch(nThreadCount);

		for (int i = 0; i < nThreadCount; i++)
		{
			new Thread(
				() ->
			{
				launch(run ->
				{
					run.blocking(cr);
					signal.countDown();
				});
			}).start();
		}

		try
		{
			signal.await();
		}
		catch (InterruptedException e)
		{
			throw new CompletionException(e);
		}

		p.measure(nThreadCount + " Threads");

		launch(
			run ->
		{
			for (int i = 0; i < nCoroutineCount; i++)
			{
				run.async(cr);
			}
		});

		p.measure(nCoroutineCount + " Coroutines");
		p.printSummary();
	}

	/***************************************
	 * Creates a new instance.
	 */
	public static void demoSocketCommunication()
	{
		InetSocketAddress server = new InetSocketAddress("example.com", 80);

		Coroutine<ByteBuffer, ByteBuffer> cr =
			first(sendTo(server)).then(receiveFrom(server));

		launch(
			run ->
			{
				ByteBuffer data = ByteBuffer.allocate(100_000);

				data.put(
					"GET /index.html HTTP/1.1\r\nHost: example.com\r\n\r\n"
						.getBytes(StandardCharsets.US_ASCII));

				data.flip();

				ByteBuffer result = run.blocking(cr, data).getResult();

				result.flip();

				byte[] bytes = new byte[result.limit()];

				result.get(bytes);

				System.out.printf(
					"RESPONSE: \n%s",
					new String(bytes, StandardCharsets.UTF_8));
			});
	}

	/***************************************
	 * Main
	 *
	 * @param rArgs
	 */
	public static void main(String[] rArgs)
	{
//		demoParallelExecution();
		demoSocketCommunication();
	}
}

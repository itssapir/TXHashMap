import org.junit.Assert;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class QueueTest {

	@Test
	public void testQueueSingleton(){
		Queue QInt = new Queue();
		Assert.assertEquals(true, QInt.isEmpty());
		int i = 0;
		try {
			i = (int) QInt.dequeue();
			Assert.fail("did not throw TXLibExceptions.QueueIsEmptyException");
		} catch (TXLibExceptions.QueueIsEmptyException ignored) {
		}
		QInt.enqueue(1);
		Assert.assertEquals(false, QInt.isEmpty());
		try {
			i = (int) QInt.dequeue();
		} catch (TXLibExceptions.QueueIsEmptyException ignored) {
		}
		Assert.assertEquals(1, i);
		try {
			i = (int) QInt.dequeue();
			Assert.fail("did not throw TXLibExceptions.QueueIsEmptyException");
		} catch (TXLibExceptions.QueueIsEmptyException ignored) {
		}
		QInt.enqueue(1);
		QInt.enqueue(2);
		QInt.enqueue(3);
		try {
			i = (int) QInt.dequeue();
		} catch (TXLibExceptions.QueueIsEmptyException ignored) {
		}
		Assert.assertEquals(1, i);
	}

	@Test
	public void testQueueSingleThreadTX() throws TXLibExceptions.AbortException, TXLibExceptions.QueueIsEmptyException {
		Integer i = 1;
		Queue Q = new Queue();
		while (true) {
			try {
				try {
					TX.TXbegin();
					Assert.assertEquals(true, Q.isEmpty());
					try {
						Q.dequeue();
						Assert.fail("did not throw TXLibExceptions.QueueIsEmptyException");
					} catch (TXLibExceptions.QueueIsEmptyException ignored) {
					}
					Q.enqueue(i);
				} finally {
					TX.TXend();
				}
			} catch (TXLibExceptions.AbortException exp) {
				Assert.fail("single thread TX should not abort");
				continue;
			}
			break;
		}
		Assert.assertEquals(i, Q.dequeue()); // singleton

		String a = "a";
		String b = "b";
		String c = "c";
		while (true) {
			try {
				try {
					TX.TXbegin();
					Assert.assertEquals(true, Q.isEmpty());
					Q.enqueue(a);
					Q.enqueue(b);
					Q.enqueue(c);
					Assert.assertEquals(a, Q.dequeue());
					Q.enqueue(a);
				} finally {
					TX.TXend();
				}
			} catch (TXLibExceptions.AbortException exp) {
				// fail("single thread TX should not abort");
				// TODO this should not abort?
				continue;
			}
			break;
		}
		Assert.assertEquals(false, Q.isEmpty());
		Assert.assertEquals(b, Q.dequeue());
		Assert.assertEquals(c, Q.dequeue());
		Assert.assertEquals(a, Q.dequeue());
		Assert.assertEquals(true, Q.isEmpty());
		while (true) {
			try {
				try {
					TX.TXbegin();
					Q.enqueue(a);
					Assert.assertEquals(a, Q.dequeue());
					Q.dequeue();
					Q.enqueue(a);
					Assert.fail("did not throw TXLibExceptions.QueueIsEmptyException");
				} catch (TXLibExceptions.QueueIsEmptyException ignored) {
				} finally {
					TX.TXend();
				}
			} catch (TXLibExceptions.AbortException exp) {
				// fail("single thread TX should not abort");
				continue;
			}
			break;
		}
		Assert.assertEquals(true, Q.isEmpty());

		Queue Q2 = new Queue();
		while (true) {
			try {
				try {
					TX.TXbegin();
					Q.enqueue(a);
					Q2.enqueue(a);
					Assert.assertEquals(a, Q.dequeue());
					Q.dequeue();
					Q.enqueue(b);
					Q2.enqueue(b);
					Assert.fail("did not throw TXLibExceptions.QueueIsEmptyException");
				} catch (TXLibExceptions.QueueIsEmptyException ignored) {
				} finally {
					TX.TXend();
				}
			} catch (TXLibExceptions.AbortException exp) {
				// fail("single thread TX should not abort");
				continue;
			}
			break;
		}
		Assert.assertEquals(true, Q.isEmpty());
		Assert.assertEquals(false, Q2.isEmpty());
		Assert.assertEquals(a, Q2.dequeue());
		Assert.assertEquals(true, Q2.isEmpty());

		Q.enqueue(c);
		Q.enqueue(b);
		while (true) {
			try {
				try {
					TX.TXbegin();
					Assert.assertEquals(c, Q.dequeue());
				} catch (TXLibExceptions.QueueIsEmptyException e) {
					Assert.fail("Queue should not be empty");
				} finally {
					TX.TXend();
				}
			} catch (TXLibExceptions.AbortException exp) {
				// fail("single thread TX should not abort");
				continue;
			}
			break;
		}
		Assert.assertEquals(false, Q.isEmpty());
		Assert.assertEquals(b, Q.dequeue());
		Assert.assertEquals(true, Q.isEmpty());

		Q.enqueue(a);

		while (true) {
			try {
				try {
					TX.TXbegin();
					Q.enqueue(b);
					Q.enqueue(c);
					Assert.assertEquals(a, Q.dequeue());
				} catch (TXLibExceptions.QueueIsEmptyException e) {
					Assert.fail("Queue should not be empty");
				} finally {
					TX.TXend();
				}
			} catch (TXLibExceptions.AbortException exp) {
				// fail("single thread TX should not abort");
				continue;
			}
			break;
		}
		Assert.assertEquals(false, Q.isEmpty());
		Assert.assertEquals(b, Q.dequeue());
		Assert.assertEquals(c, Q.dequeue());
		Assert.assertEquals(true, Q.isEmpty());

	}

	class Run implements Runnable {

		Queue Q1;
		Queue Q2;
		String threadName;
		CountDownLatch latch;

		Run(String name, CountDownLatch l, Queue q1, Queue q2) {
			threadName = name;
			latch = l;
			Q1 = q1;
			Q2 = q2;
		}

		@Override
		public void run() {
			try {
				latch.await();
			} catch (InterruptedException exp) {
				System.out.println(threadName + ": InterruptedException");
			}
			String a = threadName + "-a";
			String b = threadName + "-b";
			String c = threadName + "-c";
			while (true) {
				try {
					try {
						TX.TXbegin();
						if (!Q1.isEmpty()) {
							System.out.println(threadName + ": queue is not empty");
						}
						Q1.enqueue(a);
						Q1.enqueue(b);
						Q1.enqueue(c);
						Q2.enqueue(a);
						Q2.enqueue(b);
						Q2.enqueue(c);
						if(TX.DEBUG_MODE_QUEUE){
							System.out.println("finished enqueue in multi test");
						}
						try {
							assertEquals(a, Q1.dequeue());
							if(TX.DEBUG_MODE_QUEUE){
								System.out.println("assertEquals(a, Q1.dequeue())");
							}
							assertEquals(b, Q1.dequeue());
							if(TX.DEBUG_MODE_QUEUE){
								System.out.println("assertEquals(b, Q1.dequeue())");
							}
							assertEquals(c, Q1.dequeue());
							if(TX.DEBUG_MODE_QUEUE){
								System.out.println("assertEquals(c, Q1.dequeue())");
							}
							assertEquals(a, Q2.dequeue());
							if(TX.DEBUG_MODE_QUEUE){
								System.out.println("assertEquals(a, Q2.dequeue())");
							}
							assertEquals(b, Q2.dequeue());
							if(TX.DEBUG_MODE_QUEUE){
								System.out.println("assertEquals(b, Q2.dequeue())");
							}
							assertEquals(c, Q2.dequeue());
							if(TX.DEBUG_MODE_QUEUE){
								System.out.println("assertEquals(c, Q2.dequeue())");
							}
							if(TX.DEBUG_MODE_QUEUE){
								System.out.println("finished dequeue in multi test");
							}
						} catch (TXLibExceptions.QueueIsEmptyException exp) {
							System.out.println(threadName + ": queue is empty exception");
						}
					} finally {
						TX.TXend();
					}
				} catch (TXLibExceptions.AbortException exp) {
					continue;
				}
				break;
			}
			try {
				assertEquals(true, Q1.isEmpty());
			} catch (TXLibExceptions.AbortException exp) {
				fail("singleton should not abort");
			}
		}

	}

	@Test
	public void testQueueMultiThreadTX() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Queue Q1 = new Queue();
		Queue Q2 = new Queue();
		Thread T1 = new Thread(new Run("T1", latch, Q1, Q2));
		Thread T2 = new Thread(new Run("T2", latch, Q1, Q2));
		Thread T3 = new Thread(new Run("T3", latch, Q1, Q2));
		T1.start();
		T2.start();
		T3.start();
		latch.countDown();
		T1.join();
		T2.join();
		T3.join();
	}

}

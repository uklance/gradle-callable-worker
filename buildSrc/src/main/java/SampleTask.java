import org.gradle.api.*;
import org.gradle.api.file.*;
import org.gradle.api.provider.*;
import org.gradle.api.tasks.*;
import org.gradle.workers.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;
import java.io.*;
import java.util.concurrent.*;
import javax.inject.*;

public class SampleTask extends DefaultTask {
	private final WorkerExecutor workerExecutor; 
	
	@Inject
	public SampleTask(WorkerExecutor workerExecutor) {
		this.workerExecutor = workerExecutor;
	}
	
	@TaskAction
	public void executeCallables() {
		File resultDir = getProject().file("build/" + getName());
		CallableWorkerExecutor callableWorkerExecutor = new CallableWorkerExecutor(resultDir, workerExecutor);
		
		Stream.of("XXX", 123, 456L, new Date()).forEach(value -> {
			callableWorkerExecutor.submit(SampleCallable.class, new Action<CallableWorkerExecutor.CallableWorkerConfiguration>() {
				public void execute(CallableWorkerExecutor.CallableWorkerConfiguration config) {
					config.params(value);
				}
			});
		});
		List results = callableWorkerExecutor.await();
		
		getProject().getLogger().lifecycle("Results = " + results);
	}
	
	public static class SampleCallable implements Callable {
		private final Serializable value;
		public SampleCallable(Serializable value) {
			this.value = value;
		}
		public Object call() {
			return value;
		}
	}
}
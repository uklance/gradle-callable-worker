import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerConfiguration;
import org.gradle.workers.WorkerExecutor;

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
			callableWorkerExecutor.submit(SampleCallable.class, new Action<WorkerConfiguration>() {
				public void execute(WorkerConfiguration config) {
					config.params(value);
				}
			});
		});
		List<Object> results = callableWorkerExecutor.await();
		
		getProject().getLogger().lifecycle("Results = " + results);
	}
	
	public static class SampleCallable implements Callable<Serializable> {
		private final Serializable value;
		public SampleCallable(Serializable value) {
			this.value = value;
		}
		public Serializable call() {
			return value;
		}
	}
}
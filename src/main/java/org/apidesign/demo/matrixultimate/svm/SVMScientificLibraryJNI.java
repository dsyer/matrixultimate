package org.apidesign.demo.matrixultimate.svm;

import org.apidesign.demo.matrixultimate.MatrixSearch;
import org.apidesign.demo.matrixultimate.svm.JNIEnv.JClass;
import org.apidesign.demo.matrixultimate.svm.JNIEnv.JMethodID;
import org.apidesign.demo.matrixultimate.svm.JNIEnv.JObject;
import org.apidesign.demo.matrixultimate.svm.JNIEnv.JValue;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.Pointer;

/**
 * Native image implementation of JNI entry point methods. All the {@link CEntryPoint}
 * methods are on the "boundary" from the native side. The code in here can interface with
 * C libraries without any overhead.
 */
final class SVMScientificLibraryJNI {

	private SVMScientificLibraryJNI() {
	}

	/**
	 * Native image implementation of {@link SVMIsolate#svmInit()}
	 * @return the {@link SVMIsolate#ID ID} of the native-image VM runtime
	 */
	@CEntryPoint(name = "Java_org_apidesign_demo_matrixultimate_svm_SVMIsolate_svmInit", builtin = CEntryPoint.Builtin.CREATE_ISOLATE)
	public static native long svmInit();

	/** Native image implementation of {@link SVMScientificLibrary#create0} */
	@CEntryPoint(name = "Java_org_apidesign_demo_matrixultimate_svm_SVMScientificLibrary_create0")
	public static RawScientificLibrary.GslMatrix create0(Pointer jniEnv, Pointer clazz,
			@CEntryPoint.IsolateThreadContext long isolateId, long size1, long size2) {
		return RawScientificLibrary.gsl_matrix_alloc(size1, size2);
	}

	/** Native image implementation of {@link SVMScientificLibrary#free0} */
	@CEntryPoint(name = "Java_org_apidesign_demo_matrixultimate_svm_SVMScientificLibrary_free0")
	public static void free0(Pointer jniEnv, Pointer clazz,
			@CEntryPoint.IsolateThreadContext long isolateId,
			RawScientificLibrary.GslMatrix ptr) {
		RawScientificLibrary.gsl_matrix_free(ptr);
	}

	/** Native image implementation of {@link SVMScientificLibrary#get0} */
	@CEntryPoint(name = "Java_org_apidesign_demo_matrixultimate_svm_SVMScientificLibrary_get0")
	public static double get0(Pointer jniEnv, Pointer clazz,
			@CEntryPoint.IsolateThreadContext long isolateId,
			RawScientificLibrary.GslMatrix ptr, long r, long c) {
		return RawScientificLibrary.gsl_matrix_get(ptr, r, c);
	}

	/** Native image implementation of {@link SVMScientificLibrary#set0} */
	@CEntryPoint(name = "Java_org_apidesign_demo_matrixultimate_svm_SVMScientificLibrary_set0")
	public static void set0(Pointer jniEnv, Pointer clazz,
			@CEntryPoint.IsolateThreadContext long isolateId,
			RawScientificLibrary.GslMatrix ptr, long r, long c, double v) {
		RawScientificLibrary.gsl_matrix_set(ptr, r, c, v);
	}

	/** Native image implementation of {@link SVMScientificLibrary#size0} */
	@CEntryPoint(name = "Java_org_apidesign_demo_matrixultimate_svm_SVMScientificLibrary_size0")
	public static long size0(Pointer jniEnv, Pointer clazz,
			@CEntryPoint.IsolateThreadContext long isolateId,
			RawScientificLibrary.GslMatrix ptr, int type) {
		switch (type) {
		case 1:
			return ptr.size1();
		case 2:
			return ptr.size2();
		default:
			throw new IllegalStateException();
		}
	}

	private static final MatrixSearch FIND_BIGGEST_SQUARE = MatrixSearch
			.findBiggestSquare(RawScientificLibrary.getDefault());

	/**
	 * Implementation of JNI native method.
	 * 
	 * @see SVMBiggestSquare#directlyComputeViaSvm
	 */
	@CEntryPoint(name = "Java_org_apidesign_demo_matrixultimate_svm_SVMBiggestSquare_directlyComputeViaSvm")
	public static JObject directlyComputeViaSvm(JNIEnv env, JNIEnv.JClass clazz,
			@CEntryPoint.IsolateThreadContext long isolateId,
			RawScientificLibrary.GslMatrix ptr) {
		MatrixSearch.Result result = FIND_BIGGEST_SQUARE.search(ptr.rawValue());
		return convertSVMToJVM(env, result);
	}

	/**
	 * Native image object to HotSpot JVM object conversion. Converts
	 * {@link MatrixSearch.Result} object of <b>native-image</b> to
	 * {@link MatrixSearch.Result} object of standard JVM using {@link JNIEnv} interface
	 * to JVM.
	 * @param env the interface to the (HotSpot) JVM
	 * @param result object to convert
	 * @return JObject representing the result in the (HotSpot) JVM
	 */
	private static JObject convertSVMToJVM(JNIEnv env, MatrixSearch.Result result) {
		JNIEnv.JNINativeInterface fn = env.getFunctions();
		final String resultClassNameJava = MatrixSearch.Result.class.getName()
				.replace('.', '/');
		try (CTypeConversion.CCharPointerHolder resultClassName = CTypeConversion
				.toCString(resultClassNameJava);
				CTypeConversion.CCharPointerHolder name = CTypeConversion
						.toCString("<init>");
				CTypeConversion.CCharPointerHolder sig = CTypeConversion
						.toCString("(JJJJ)V");) {
			JClass resultClass = fn.getFindClass().find(env, resultClassName.get());
			JMethodID constuctor = fn.getGetMethodID().find(env, resultClass, name.get(),
					sig.get());

			JValue args = StackValue.get(4, JValue.class);
			args.addressOf(0).j(result.getRow());
			args.addressOf(1).j(result.getColumn());
			args.addressOf(2).j(result.getSize());
			args.addressOf(3).j(result.getMilliseconds());

			JObject jvmResult = fn.getNewObjectA().call(env, resultClass, constuctor,
					args);
			return jvmResult;
		}
	}

}

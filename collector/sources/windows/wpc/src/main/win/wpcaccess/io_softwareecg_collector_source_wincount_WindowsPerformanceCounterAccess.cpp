//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Josef Adersberger
//       creation date: 2010
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-03-04 16:00:42 +0100 (Do, 04 Mrz 2010) $
//       revision:      $Revision: 419 $
//       description:   Windows performance counter JNI wrapper.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________

#include<windows.h>
#include<pdh.h>
#include<pdhmsg.h>
#include<map>
#include<string>
#include<iostream>
#include<vector>

using namespace std;
#include"io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess.h"

CRITICAL_SECTION cs;

/*
 * Class:     io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess
 */
JNIEXPORT void JNICALL Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_init (JNIEnv * env, jclass clazz) {
	InitializeCriticalSection(&cs);
}

//______________________________________________________________________________
//
// convert jstring to wchar_t - make sure the w_char[] is after use deleted.
//______________________________________________________________________________
const wchar_t* toLPCWSTR(JNIEnv*env, jstring s) {
    if (s == NULL) return NULL;
    jboolean isCopy = false;
    const jchar * r = env -> GetStringChars(s, & isCopy);
    jsize sz = env -> GetStringLength(s);
    wchar_t * buf = new wchar_t[sz + 2];
    memcpy(buf, r, sz * sizeof(wchar_t));
    buf[sz] = L'\0';
    env -> ReleaseStringChars(s, r);
    return buf;
}

//______________________________________________________________________________
//
// convert const char * to jstring
//______________________________________________________________________________
 jstring JNU_NewStringNative(JNIEnv*env, const char*str) {
     jstring result;
     jbyteArray bytes = 0;
     size_t len;
     if (env -> EnsureLocalCapacity(2) < 0) {
         return NULL; /* out of memory error */
     }
     len = strlen(str);
     bytes = env -> NewByteArray(len);
     if (bytes != NULL) {
         env -> SetByteArrayRegion(bytes, 0, len,
                 (jbyte *)str);
         jclass stringClass = env -> FindClass("java/lang/String");

         jmethodID constructor = env -> GetMethodID(stringClass, "<init>", "([C)V");
         result = (jstring) env -> NewObject(stringClass, constructor, bytes);
         env -> DeleteLocalRef(bytes);
         env -> DeleteLocalRef(stringClass);
         return result;
     } /* else fall through */
     return NULL;
 }

//______________________________________________________________________________
//
// Native implementation of for the WindowsPerformanceCounter EKG Source
//______________________________________________________________________________


//
// Holder for counter information
//
struct Counter {
    HCOUNTER hCounter;
    HQUERY hQuery;
};

//
// Counter cache
//
map<wstring, Counter> counterMap;

/*
* Class:     io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess
* Method:    getCounter
* Signature: (Ljava/lang/String;Ljava/lang/String;)D
*/
JNIEXPORT jdouble

JNICALL Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_getCounter
        (JNIEnv*env, jobject obj, jstring counter) {
    PDH_FMT_COUNTERVALUE pValue = {0};
    PDH_STATUS pdhStatus = {0};
    Counter counterInfo = {0};
    const TCHAR * szCounterPath = toLPCWSTR(env, counter);

    if (szCounterPath == NULL) {
        jclass Exception = env -> FindClass("java/lang/RuntimeException");
        env -> ThrowNew(Exception, "Out of memory.");
        goto exit;
    }

    // Aquire Java monitor to protect accesses to counterMap ----------------------------------------------------------
    if (env -> MonitorEnter(obj) != JNI_OK) return -1;

    // only once
    if (counterMap.find(szCounterPath) == counterMap.end()) {

        // Open a query object.
        pdhStatus = PdhOpenQuery(NULL, 0, & (counterInfo.hQuery));

        if (pdhStatus != ERROR_SUCCESS) {
            jclass Exception = env -> FindClass("java/lang/RuntimeException");
            env -> ThrowNew(Exception, "Can not open query.");
            // Cleanup Monitor, exit
            env -> MonitorExit(obj);
            goto exit;
        }

        // Add one counter that will provide the data.
        pdhStatus = PdhAddCounter(counterInfo.hQuery,
                szCounterPath,
                0,
                & (counterInfo.hCounter));

        if (pdhStatus != ERROR_SUCCESS) {
            pdhStatus = PdhCloseQuery(counterInfo.hQuery);
            // ignore pdhStatus

            jclass Exception = env -> FindClass("java/lang/RuntimeException");
            env -> ThrowNew(Exception, "Can not add counter.");
            // Cleanup Monitor, exit
            env -> MonitorExit(obj);
            goto exit;
        }
        counterMap[szCounterPath] = counterInfo;
    } else {
        counterInfo = counterMap[szCounterPath];
    }

    // Release Java monitor ------------------------------------------------------------------------------------------
    if (env -> MonitorExit(obj) != JNI_OK) return -1;

    pdhStatus =::PdhCollectQueryData(counterInfo.hQuery);
    if (pdhStatus != ERROR_SUCCESS) {
        //jclass Exception = env -> FindClass("java/lang/RuntimeException");
        //env -> ThrowNew(Exception, "Can not collect counter data.");
		pValue.doubleValue = -1.0;
		goto exit;
    }

    DWORD dwType;
    pdhStatus =::PdhGetFormattedCounterValue(counterInfo.hCounter, PDH_FMT_DOUBLE, & dwType,&pValue);
    if (pdhStatus != ERROR_SUCCESS) {
        //jclass Exception = env -> FindClass("java/lang/RuntimeException");
        //env -> ThrowNew(Exception, "Can not access counter value.");
		pValue.doubleValue = -1.0;
        goto exit;
    }

    exit:
    if (szCounterPath != NULL) {
        delete[] szCounterPath;
    }

    return pValue.doubleValue;
}


JNIEXPORT jboolean

JNICALL Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_authenticate
        (JNIEnv*env, jobject thisObject, jstring domain, jstring userName, jstring password) {
    HANDLE token = 0;
    HANDLE tokenDuplicate = 0;
    LPCWSTR user = toLPCWSTR(env, userName);
    LPCWSTR pwd = toLPCWSTR(env, password);
    LPCWSTR dom = toLPCWSTR(env, domain);
    jboolean success = false;

    if (LogonUser(user, dom, pwd, 9, 0, & token)!=0)
    {
        if (DuplicateToken(token, SecurityImpersonation, & tokenDuplicate)!=0)
        {
            success = ImpersonateLoggedOnUser(tokenDuplicate);
        }
    }else{
        jclass Exception = env -> FindClass("java/lang/Exception");
        env -> ThrowNew(Exception, "Can not authenticate.");
    }
    if (token != 0) {
        CloseHandle(token);
    }

    if (tokenDuplicate != 0) {
        CloseHandle(tokenDuplicate);
    }
    delete[] user;
    delete[] pwd;
    delete[] dom;
    return success;
}


/*
* Class:     io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess
* Method:    resolveWildcards
* Signature: (Ljava/lang/String;)[Ljava/lang/String;
*/
JNIEXPORT jobjectArray
JNICALL Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_resolveWildcards
        (JNIEnv*env, jobject thisObject, jstring wildcardString) {

	jobjectArray result = 0;

	// Aquire monitor
 	EnterCriticalSection(&cs);

	LPCWSTR szWildCardPath = toLPCWSTR(env, wildcardString);

    DWORD flags = 0;
    DWORD size = 0;
	const char * MESSAGE = 0;
	PDH_STATUS pdhStatus = 0;
	PWSTR path = NULL;
	// declared before the first 'goto exit': a jump must not cross an initialization
	int count = 0;
	int i = 0;

	// PdhExpandWildCardPath has to be called twice. This is not a error. 
    // see http://msdn.microsoft.com/en-us/library/aa372606(VS.85).aspx
 
	pdhStatus = PdhExpandCounterPath(szWildCardPath, path, &size);
    while (pdhStatus == PDH_MORE_DATA) 
    {
        if (path != NULL) free(path);
		path = (PWSTR) malloc((size + 2) * sizeof(WCHAR)); // +1 for win < vista
		memset(path, 0, (size + 2) * sizeof(WCHAR));
        pdhStatus = PdhExpandWildCardPath(NULL, szWildCardPath, path, &size, 0);
    }
	if (pdhStatus != ERROR_SUCCESS) {
		if (pdhStatus == PDH_INVALID_ARGUMENT) {
			MESSAGE = "PdhExpandWildCardPath returned PDH_INVALID_ARGUMENT. Check your counter definitions.";
		} else if (pdhStatus == PDH_MEMORY_ALLOCATION_FAILURE) {
			MESSAGE = "PdhExpandWildCardPath returned PDH_MEMORY_ALLOCATION_FAILURE. This should never happen.";
		} else if (pdhStatus == PDH_CSTATUS_NO_OBJECT) {
			MESSAGE = "PdhExpandWildCardPath returned PDH_CSTATUS_NO_OBJECT. Check your counter definition";
		}
        goto exit;
    }
	if (path == NULL) {
		// nothing was expanded: there is no buffer to read, the caller gets no result
		goto exit;
	}

	// Process buffer and store each element in the result array.

    // On Vista and later operating systems, the buffer is terminated with two 
    // null-terminator characters; however, on earlier systems, the buffer is
    // not terminated with two null-terminator characters. This covers both cases.

	for (PWSTR p = path; ((p != p + size) && (*p != L'\0')); p += wcslen(p) + 1) 
    {
		count++;
    }
	result = (jobjectArray) env -> NewObjectArray(count,
            env -> FindClass("java/lang/String"),
            env -> NewStringUTF(""));

	for (PWSTR p = path; ((p != p + size) && (*p != L'\0')); p += wcslen(p) + 1) 
    {
		jstring s = env -> NewString((jchar *)p, wcslen(p));
        env -> SetObjectArrayElement(result, i++, s);
    }

    exit:
	if (path) {
		free(path);
	}
	delete[] szWildCardPath;
	
	LeaveCriticalSection(&cs);
	
	if (MESSAGE) {
		jclass Exception = env -> FindClass("java/lang/RuntimeException");
		env -> ThrowNew(Exception, MESSAGE);			
    }
    return result;
}

//
// maps query handles to counter handles
//
map<HQUERY, vector<HCOUNTER>*> query2Counter;

/*
* Class:     io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess
* Method:    queryCounter
* Signature: (J[Ljava/lang/String;[D)J
*/
JNIEXPORT jlong
JNICALL Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_queryCounter
        (JNIEnv*env, jobject thisObject, jlong queryHandle, jobjectArray counterStrings, jdoubleArray resultArray) {

	// Aquire monitor
 	// EnterCriticalSection(&cs);
 
	PDH_STATUS pdhStatus = {0};

    HQUERY hQueryHandle = (HQUERY) queryHandle;

	jdouble *elements = env -> GetDoubleArrayElements(resultArray, 0);

    int counterStringsLength = env -> GetArrayLength(counterStrings);
	if (counterStringsLength == 0) {
		goto exit;
	}

	// check if handle is already in map
    if (query2Counter.find(hQueryHandle) == query2Counter.end()) {

		// cout << "Query handle '" << hQueryHandle << "' not found. Creating new query." << endl;
		
		// new query 
        pdhStatus = PdhOpenQuery(NULL, 0, & hQueryHandle);

        if (pdhStatus != ERROR_SUCCESS) {
            jclass Exception = env -> FindClass("java/lang/RuntimeException");
            env -> ThrowNew(Exception, "Can not open query.");			
			env -> DeleteLocalRef(Exception);
            goto exit;
        }

		vector<HCOUNTER> * counterV = new vector<HCOUNTER>();
		query2Counter[hQueryHandle] = counterV;

        for (int i = 0; i < counterStringsLength; i++) {
            jstring jstr = (jstring) env -> GetObjectArrayElement(counterStrings, i);

            LPCWSTR szCounterPath = toLPCWSTR(env, jstr);
			env -> DeleteLocalRef(jstr);

            // stays NULL if the counter cannot be added: its value is then read as an error (-1)
            HCOUNTER hCounter = NULL;
            // Add one counter that will provide the data.
            pdhStatus = PdhAddCounter(hQueryHandle,
                    szCounterPath,
                    0,
                    & hCounter);

            delete[] szCounterPath;
			
            if (pdhStatus != ERROR_SUCCESS) {
                cerr << "Can not add counter '" << szCounterPath << "' to query" << endl;
            }
            counterV->push_back(hCounter); 
        }	
    }

    // request all values for the query
    pdhStatus = ::PdhCollectQueryData(hQueryHandle);
    if (pdhStatus != ERROR_SUCCESS) {
		// cerr << "PdhCollectQueryData returned PDH_STATUS: " << pdhStatus << ". This is normal when all counters of a query can not accessed." << endl; 
		// goto exit;
	}
	
    // store result values
    for (int i = 0; i < counterStringsLength; i++) {
        PDH_FMT_COUNTERVALUE pValue = {0};
        DWORD dwType;
        pdhStatus = ::PdhGetFormattedCounterValue(query2Counter[hQueryHandle]->at(i), PDH_FMT_DOUBLE | PDH_FMT_NOCAP100, &dwType, &pValue);
        double result = pValue.doubleValue;
        if (pdhStatus != ERROR_SUCCESS) {
            result = -1; // error 
        }
        // store result in result array
		elements[i] = result;
    } 

    exit:
	env -> ReleaseDoubleArrayElements(resultArray, elements, 0);
	env -> DeleteLocalRef(resultArray);
		
	//LeaveCriticalSection(&cs);

    return (jlong) hQueryHandle;
}

JNIEXPORT void
JNICALL Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_deleteQuery
        (JNIEnv*env, jobject thisObject, jlong queryHandle) {
    HQUERY hQueryHandle = (HQUERY) queryHandle;

	// count is 0 if there is no query
    if (!query2Counter.count(hQueryHandle)) {	
		query2Counter.erase(hQueryHandle);
		::PdhCloseQuery(hQueryHandle);
	}
}
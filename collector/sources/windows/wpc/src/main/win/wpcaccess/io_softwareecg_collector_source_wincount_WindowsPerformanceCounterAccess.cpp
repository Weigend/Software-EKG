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

// set once the critical section exists: init is called for every Java object
static volatile LONG csInitialized = 0;

/*
 * Class:     io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess
 */
JNIEXPORT void JNICALL Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_init (JNIEnv * env, jclass clazz) {
	if (InterlockedCompareExchange(&csInitialized, 1, 0) == 0) {
		InitializeCriticalSection(&cs);
	}
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

	// the counters of the query; declared before the first 'goto exit'
	vector<HCOUNTER> * counterV = NULL;

    int counterStringsLength = env -> GetArrayLength(counterStrings);
	if (counterStringsLength == 0) {
		goto exit;
	}

	// check if handle is already in map. The map is shared by the threads of all jobs.
	EnterCriticalSection(&cs);
	{
		map<HQUERY, vector<HCOUNTER>*>::iterator known = query2Counter.find(hQueryHandle);
		if (known != query2Counter.end()) {
			counterV = known->second;
		}
	}
	LeaveCriticalSection(&cs);

    if (counterV == NULL) {

		// new query 
        pdhStatus = PdhOpenQuery(NULL, 0, & hQueryHandle);

        if (pdhStatus != ERROR_SUCCESS) {
            jclass Exception = env -> FindClass("java/lang/RuntimeException");
            env -> ThrowNew(Exception, "Can not open query.");			
			env -> DeleteLocalRef(Exception);
            goto exit;
        }

		counterV = new vector<HCOUNTER>();

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
                cerr << "Can not add counter " << i << " to query" << endl;
            }
            counterV->push_back(hCounter); 
        }	

		EnterCriticalSection(&cs);
		query2Counter[hQueryHandle] = counterV;
		LeaveCriticalSection(&cs);
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
        // a query knows the counters it was made with; one asked for beyond them has no value
        if ((size_t) i < counterV->size()) {
            pdhStatus = ::PdhGetFormattedCounterValue(counterV->at(i), PDH_FMT_DOUBLE | PDH_FMT_NOCAP100, &dwType, &pValue);
        } else {
            pdhStatus = PDH_INVALID_HANDLE;
        }
        double result = pValue.doubleValue;
        if (pdhStatus != (PDH_STATUS) ERROR_SUCCESS) {
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
    vector<HCOUNTER> * counterV = NULL;

    // only a query that was made here is closed, and only once
    EnterCriticalSection(&cs);
    {
        map<HQUERY, vector<HCOUNTER>*>::iterator known = query2Counter.find(hQueryHandle);
        if (known != query2Counter.end()) {
            counterV = known->second;
            query2Counter.erase(known);
        }
    }
    LeaveCriticalSection(&cs);

    if (counterV != NULL) {
        ::PdhCloseQuery(hQueryHandle);
        delete counterV;
    }
}


//______________________________________________________________________________
//
// The names of the performance objects and counters. A counter path is written
// in the language of the Windows it is read on; these two functions let the
// Java side turn the English names into the ones of this machine.
//______________________________________________________________________________

/*
* Class:     io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess
* Method:    englishNames
* Signature: ()[Ljava/lang/String;
*
* The English names of all objects and counters with their indexes, as Windows
* keeps them: index, name, index, name ... NULL if they cannot be read.
*/
JNIEXPORT jobjectArray
JNICALL Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_englishNames
        (JNIEnv*env, jobject thisObject) {

    jobjectArray result = NULL;
    HKEY key = NULL;
    wchar_t * buffer = NULL;
    DWORD bytes = 0;
    DWORD type = 0;
    LONG status = ERROR_SUCCESS;
    size_t chars = 0;
    int count = 0;
    int index = 0;
    jclass stringClass = NULL;

    // 009 is English; the key exists on a Windows of every language
    status = RegOpenKeyExW(HKEY_LOCAL_MACHINE,
            L"SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009", 0, KEY_READ, &key);
    if (status != ERROR_SUCCESS) {
        return NULL;
    }
    status = RegQueryValueExW(key, L"Counter", NULL, &type, NULL, &bytes);
    if ((status != ERROR_SUCCESS && status != ERROR_MORE_DATA) || type != REG_MULTI_SZ || bytes == 0) {
        goto exit;
    }
    // two more characters: the registry does not promise the terminators of a list of strings
    chars = bytes / sizeof(wchar_t);
    buffer = (wchar_t *) calloc(chars + 2, sizeof(wchar_t));
    if (buffer == NULL) {
        goto exit;
    }
    status = RegQueryValueExW(key, L"Counter", NULL, &type, (LPBYTE) buffer, &bytes);
    if (status != ERROR_SUCCESS) {
        goto exit;
    }

    for (wchar_t * p = buffer; *p != L'\0'; p += wcslen(p) + 1) {
        count++;
    }
    stringClass = env -> FindClass("java/lang/String");
    result = env -> NewObjectArray(count, stringClass, NULL);
    if (result == NULL) {
        goto exit;
    }
    for (wchar_t * p = buffer; *p != L'\0' && index < count; p += wcslen(p) + 1) {
        jstring s = env -> NewString((const jchar *) p, (jsize) wcslen(p));
        env -> SetObjectArrayElement(result, index++, s);
        env -> DeleteLocalRef(s);
    }

    exit:
    if (buffer != NULL) {
        free(buffer);
    }
    RegCloseKey(key);
    return result;
}

/*
* Class:     io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess
* Method:    lookupName
* Signature: (I)Ljava/lang/String;
*
* The name of the object or counter with this index in the language of this
* Windows. NULL if there is none.
*/
JNIEXPORT jstring
JNICALL Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_lookupName
        (JNIEnv*env, jobject thisObject, jint index) {

    wchar_t name[PDH_MAX_COUNTER_NAME];
    DWORD size = PDH_MAX_COUNTER_NAME;
    name[0] = L'\0';
    PDH_STATUS pdhStatus = PdhLookupPerfNameByIndexW(NULL, (DWORD) index, name, &size);
    if (pdhStatus != (PDH_STATUS) ERROR_SUCCESS || name[0] == L'\0') {
        return NULL;
    }
    return env -> NewString((const jchar *) name, (jsize) wcslen(name));
}

package cl.duocuc.evaluacion2

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.collectWhileSubscribed(flow: Flow<T>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        flow.collect {}
    }
}

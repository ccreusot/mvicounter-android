package fr.cedriccreusot.mvicounter

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.RecursiveAction

interface State
interface Intent
interface ReduceAction

abstract class Store<S: State, I: Intent, A: ReduceAction>(initialState: S) {
    protected abstract val coroutineScope: CoroutineScope

    private val _state = MutableStateFlow(initialState)
    val state : StateFlow<S> = _state

    private val intentFlow = MutableSharedFlow<I>(extraBufferCapacity = 1)
    private val reducerFlow = MutableSharedFlow<A>(extraBufferCapacity = 1)

    init {
        @Suppress("LeakingThis")
        intentFlow.onEach { intent ->
            handleAction(executeIntent(intent))
        }.launchIn(coroutineScope)
        @Suppress("LeakingThis")
        reducerFlow.onEach { action ->
            _state.value = reduce(state.value, action)
        }.launchIn(coroutineScope)
    }

    fun onIntent(intent: I) {
        intentFlow.tryEmit(intent)
    }

    private fun handleAction(action: A) {
        reducerFlow.tryEmit(action)
    }

    protected abstract suspend fun executeIntent(intent: I): A
    protected abstract fun reduce(state: S, reduceAction: A) : S
}

sealed class CounterIntent: Intent {
    object IncrementCounterIntent : CounterIntent()
}

sealed class CounterAction: ReduceAction {
    data class IncrementBy(val increaseValue: Int): CounterAction()
}

data class CountState(val value: Int) : State {
    companion object {
        val initial = CountState(0)
    }
}

class CounterStore(override val coroutineScope: CoroutineScope) : Store<CountState, CounterIntent, CounterAction>(CountState.initial) {
    override suspend fun executeIntent(intent: CounterIntent): CounterAction {
        return when (intent) {
            CounterIntent.IncrementCounterIntent -> CounterAction.IncrementBy(1)
        }
    }

    override fun reduce(state: CountState, reduceAction: CounterAction): CountState {
        return when (reduceAction) {
            is CounterAction.IncrementBy -> state.copy(value =  state.value + reduceAction.increaseValue)
        }
    }
}

class CounterViewModel: ViewModel() {
    private val store = CounterStore(viewModelScope)

    val counter: LiveData<CountState> = flow {
        store.state.collect { state ->
            emit(state)
        }
    }.asLiveData()

    fun increment() {
        store.onIntent(CounterIntent.IncrementCounterIntent)
    }
}

class MainActivity : AppCompatActivity() {

    private val counterViewModel = CounterViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        counterViewModel.counter.observe(this) { state ->
            findViewById<TextView>(R.id.counterTextView).text = "${state.value}"
        }

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            counterViewModel.increment()
        }
    }
}

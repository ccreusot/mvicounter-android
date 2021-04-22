package fr.cedriccreusot.mvicounter

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface State
interface Intent
interface ReduceAction

abstract class Store<S: State, I: Intent, A: ReduceAction>(coroutineScope: CoroutineScope, initialState: S) {

    private val _state = MutableStateFlow(initialState)
    val state : StateFlow<S> = _state

    private val intentFlow = MutableSharedFlow<I>(extraBufferCapacity = 1)
    private val reducerFlow = MutableSharedFlow<A>(extraBufferCapacity = 1)

    init {
        intentFlow.onEach { intent ->
            handleAction(executeIntent(intent))
        }.launchIn(coroutineScope)
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

class CounterStore(coroutineScope: CoroutineScope) : Store<CountState, CounterIntent, CounterAction>(coroutineScope, CountState.initial) {
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

class MainActivity : AppCompatActivity() {
    private val store = CounterStore(lifecycleScope)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        store.state.flowWithLifecycle(this.lifecycle, Lifecycle.State.CREATED).onEach { state ->
            findViewById<TextView>(R.id.counterTextView).text = "${state.value}"
        }.launchIn(lifecycleScope)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            store.onIntent(CounterIntent.IncrementCounterIntent)
        }
    }
}

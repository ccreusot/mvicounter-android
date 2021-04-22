# Small implementation of a counter app with the MVI pattern

I've made this small project after hearing about MVI on the Talking Kotlin podcast
It is inspired by this article [Implementing MVI on Android with Coroutines](https://fvilarino.medium.com/implementing-mvi-on-android-with-coroutines-7b747ef96870)
I've made small improvement on decoupling the ViewModel and the Store which in my point of view is better because I still want be able to use the LiveData since there are bound to the lifecycle.

[Reference about StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)

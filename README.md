# Small implementation of a counter app with the MVI pattern

I've made this small project after hearing about MVI on the Talking Kotlin podcast

It is inspired by this article [Implementing MVI on Android with Coroutines](https://fvilarino.medium.com/implementing-mvi-on-android-with-coroutines-7b747ef96870)

I've made small improvement by not using the ViewModel since with the current evolution of the `lifecycle-runtime-ktx` API you can bind your StateFlow and it's taking the lifecycle into account.

[Reference Lifecycle API](https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda)

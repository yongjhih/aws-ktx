package awx.lambda

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaDataBinder
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory

inline fun <reified T> LambdaInvokerFactory.build(): T =
    build(T::class.java)

inline fun <reified T> LambdaInvokerFactory.build(dataBinder: LambdaDataBinder): T =
    build(T::class.java, dataBinder)
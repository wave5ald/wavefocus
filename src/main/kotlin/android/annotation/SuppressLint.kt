package android.annotation

/** Desktop stand-in for android.annotation.SuppressLint. */
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.CONSTRUCTOR
)
annotation class SuppressLint(vararg val value: String)

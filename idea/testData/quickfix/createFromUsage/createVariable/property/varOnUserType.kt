// "Create member property 'foo'" "true"
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T)

fun test() {
    A(1).<caret>foo = "1"
}

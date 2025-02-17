package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class KnotWatchChangeTest {

    private object State

    private sealed class Change {
        object One : Change()
        object Two : Change()
    }

    private object Action

    @Test
    fun `changes { watchAll } receives external Change`() {
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this.only }
                watchAll { watcher.onNext(it) }
            }
        }
        knot.change.accept(Change.One)
        knot.change.accept(Change.Two)
        observer.assertValues(
            Change.One,
            Change.Two
        )
    }

    @Test
    fun `changes { watchAll } receives change from Action`() {
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.One -> this + Action
                        is Change.Two -> this.only
                    }
                }
                watchAll { watcher.onNext(it) }
            }
            actions {
                perform<Action> { flatMap { Observable.fromArray(Change.Two, Change.Two) } }
            }
        }
        knot.change.accept(Change.One)
        observer.assertValues(
            Change.One,
            Change.Two,
            Change.Two
        )
    }

    @Test
    fun `changes { watchAll } receives change from Event`() {
        val externalSource = PublishSubject.create<Unit>()
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this.only }
                watchAll { watcher.onNext(it) }
            }
            events {
                source { externalSource.map { Change.Two } }
            }
        }
        knot.change.accept(Change.One)
        externalSource.onNext(Unit)
        observer.assertValues(
            Change.One,
            Change.Two
        )
    }

    @Test
    fun `changes { watch } receives Change`() {
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this.only }
                watch<Change.One> { watcher.onNext(it) }
                watch<Change.Two> { watcher.onNext(it) }
            }
        }
        knot.change.accept(Change.One)
        knot.change.accept(Change.Two)
        observer.assertValues(
            Change.One,
            Change.Two
        )
    }

    @Test
    fun `changes { watch } filters Change`() {
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this.only }
                watch<Change.Two> { watcher.onNext(it) }
            }
        }
        knot.change.accept(Change.One)
        knot.change.accept(Change.Two)
        observer.assertValues(
            Change.Two
        )
    }
}

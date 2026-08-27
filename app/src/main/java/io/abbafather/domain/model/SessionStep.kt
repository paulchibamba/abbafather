package io.abbafather.domain.model

/**
 * A place a session can be in.
 *
 * A breathing pause is one of them rather than a property of the line before it, so the position a
 * session keeps is a single index into [Prayer.sessionSteps] — see `docs/DECISIONS.md`.
 */
sealed interface SessionStep {

    data class Line(val lineIndex: Int) : SessionStep

    /** The rest before [nextMovementIndex] begins. There is none after the last movement. */
    data class Pause(val nextMovementIndex: Int) : SessionStep
}

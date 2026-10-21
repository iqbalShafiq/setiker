package data.sync

/**
 * The last known connectivity of the device.
 *
 * This is a process-level fact, not screen state: the network monitor keeps it up to date, and
 * the error classifier asks it whether a failed connection means "the device is offline" or "the
 * server could not be reached". Those are different problems with different fixes, and telling a
 * user who is online that they are offline is the kind of message that costs trust.
 *
 * The default is "not known to be online", so a failure before the monitor starts is reported as
 * the more cautious of the two.
 */
object ConnectivityState {

    @Volatile
    var isOnline: Boolean = false
        internal set

    internal fun update(online: Boolean) {
        isOnline = online
    }
}

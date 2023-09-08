Release Notes
=

Versioning Policy
==
15

The convention for version numbers is major.minor.build.

* major is incremented when the public interface changes incompatibly. For example, a method is removed, or its signature changes. Clients using your library need to take care when using a library with a different major version, because things may break.
* minor is incremented when the public interface changes in a compatible way. For example, a method is added. Clients do not need to worry about about using the new version, as all the functions they are used to seeing will still be there and act the same.
* build is incremented when the implementation of a function changes, but no signatures are added or removed. For example, you found a bug and fixed it. Clients should probably update to the new version, but if it doesn't work because they depended on the broken behavior, they can easily downgrade.

1.10.19
==



1.10.18
==

1. External IP rotation
2. Closing all privacy contexts explicitly
3. Bug fix

1.10.17
==

1. Advanced proxy support: socks, authentication
2. Fix test bugs on Windows

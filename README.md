# Consensus-Protocol

Implementation of a consensus protocol that tolerates participant failures.
The protocol involves two types of processes: a coordinator, whose role is to initiate a run of
the consensus algorithm and collect the outcome of the vote; and a participant, which contributes a
vote and communicates with the other participants to agree on an outcome. The application
consists of 1 coordinator process and N participant processes, out of which any number of participants
may fail during the run of the consensus algorithm. The actual consensus algorithm is run among the
participant processes, with the coordinator only collecting outcomes from the participants. The application also
includes a logger server where the other processes send log messages over the UDP protocol.

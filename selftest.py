#!/usr/bin/env python
from subprocess import Popen
from argparse import ArgumentParser

ap = ArgumentParser()

ap.add_argument('-I', '--misc-include',
        help="Additional argfiles for configuration",
        required=True)

ap.add_argument('-o', '--output', default='selftest.log')

options = ap.parse_args()

# Build the commandline
def build_command():
    cmd = []
    cmd.append('./stester')
    cmd += ['-I', options.misc_include]

    return cmd

def run_one(cmdlist):
    cmd = build_command() + cmdlist[::]
    cmd += ['--overwrite-output', '-o', options.output]
    po = Popen(cmd)
    po.communicate()
    assert po.returncode == 0
    po = Popen(['./logview', '-f', options.output])
    po.communicate()
    assert po.returncode == 0


COMMON_OPTIONS = [
    '--scenario-ramp', '1',
    '--scenario-rebound', '1'
]

# Basic 'wait' test
run_one(['-c', 'base.Raw', '--bsc-wait', '1'])

# Basic 'wait' test with views
run_one(['-c', 'base.Raw', '--workload', 'views', '--bsc-wait', '1'])

# Rebalance in
run_one(['-c', 'rebalance.Once',
         '--rebalance-mode', 'in'
         ] + COMMON_OPTIONS)

# Rebalance out
run_one(['-c', 'rebalance.Once',
         '--rebalance-mode', 'out',
         ] + COMMON_OPTIONS)

# Swap rebalance
run_one(['-c', 'rebalance.Once',
         '--rebalance-mode', 'swap' ] + COMMON_OPTIONS)

# Failover and rebalance
run_one(['-c', 'failover.Once',
         '--failover-ept', '--failover-count', '2',
         '--failover-action-delay', '2',
         '--failover-next-action', 'FO_REBALANCE'] + COMMON_OPTIONS)

# Failover EPT and eject
run_one(['-c', 'failover.Once',
         '--failover-count', '1', '--failover-ept',
         '--failover-action-delay', '2',
         '--failover-next-action', 'FO_EJECT'] + COMMON_OPTIONS)


# Failover and readd
run_one(['-c', 'failover.Once',
         '--failover-count', '2',
         '--failover-action-delay', '2',
         '--failover-next-action', 'FO_READD'] + COMMON_OPTIONS)

import itertools
from jscheduler.constants import *


class KVVariant(object):
    def __init__(self, *args, **kwargs):
        self.cluster_count = kwargs['cluster_count']
        self.username = kwargs['username']
        self.password = kwargs['password']
        self.buckets = BUCKETS
        self.buckets_auth = BUCKETAUTH
        self.ept = EPT
        self.opmodes = OPMODES
        self.wlnames = WORKLOADS
        self.instances = INSTANCES
        self.nodes = kwargs['nodes']
        self.altaddr = False
        if kwargs['altaddr']:
            self.altaddr = kwargs['altaddr']

    def _get_cluster_combs(self):
        args = []
        cluster_combs = [ssl for ssl in SSL]
        for cluster_comb in cluster_combs:
            cluster = Cluster(nodes=self.nodes, username=self.username, password=self.password, ssl=cluster_comb)
            cluster_arg = cluster.build_args()
            args.append(cluster_arg)
        return args

    def _get_workload_combs(self):
        args = []
        cluster_args = self._get_cluster_combs()
        for arg in cluster_args:
            workload_arg = arg
            workload_combs = [(name, count, mode) for name in self.wlnames for count in self.instances for mode in self.opmodes]
            for workload_comb in workload_combs:
                bucket = Workload(workload=workload_comb[0], instance_count=workload_comb[1], mode=workload_comb[2])
                new_arg = workload_arg + bucket.build_args()
                args.append(new_arg)
        return args

    def _get_bucket_combs(self):
        args = []
        workload_args = self._get_workload_combs()
        for arg in workload_args:
            bucket_arg = arg
            bucket_combs = [(bucket, auth) for bucket in self.buckets for auth in self.buckets_auth]
            for bucket_comb in bucket_combs:
                bucket = Bucket(bucket_type=bucket_comb[0], bucket_auth=bucket_comb[1])
                new_arg = bucket_arg + bucket.build_args()
                args.append(new_arg)
        return args

    def get_failover_variants(self):
        variants = []
        bucket_args = self._get_bucket_combs()
        for arg in bucket_args:
            command = ""
            command += arg
            failover_combs = [(count, ept, next_action) for count in range(1, self.cluster_count - 1) for ept in self.ept
                             for next_action in FAILOVERACTIONS]

            for failover_comb in failover_combs:
                failover = Failover(count=failover_comb[0], ept=failover_comb[1], failover_next_action=failover_comb[2])
                new_arg = command + failover.build_args()
                variants.append(new_arg)
        return variants

    def get_rebalance_variants(self):
        variants = []
        bucket_args = self._get_bucket_combs()
        for arg in bucket_args:
            command = ""
            command += arg
            rebalance_combs = [(count, ept, mode) for count in range(1, self.cluster_count - 1) for ept in self.ept
                              for mode in REBALANCEMODE]

            for rebalance_comb in rebalance_combs:
                rebalance = Rebalance(count=rebalance_comb[0], ept=rebalance_comb[1], rebalance_mode=rebalance_comb[2])
                new_arg = command + rebalance.build_args()
                variants.append(new_arg)
        return variants

    def get_svcfailure_variants(self):
        variants = []
        bucket_args = self._get_bucket_combs()
        for arg in bucket_args:
            command = ""
            command += arg
            svcfailurecombs = [(count, name, process) for count in range(1, self.cluster_count - 1)
                               for name in SERVICEACTIONS
                               for process in PROCESSTYPE]

            for svcfailurecomb in svcfailurecombs:
                svcfailure = ServiceFailure(count=svcfailurecomb[0], service_action=svcfailurecomb[1],
                                            service_name=svcfailurecomb[2])
                new_arg = command + svcfailure.build_args()
                variants.append(new_arg)
        return variants


class Failover(object):
    def __init__(self, *args, **kwargs):
        self.count = kwargs['count']
        self.ept = kwargs['ept']
        self.failover_next_action = kwargs['failover_next_action']
        self.next_delay = FAILOVER_DELAY

    def build_args(self):
        command = " --failover-next-action {0}".format(self.failover_next_action)
        command += " --rebalance-ept {0}".format(self.ept)
        command += " --failover-count {0}".format(self.count)
        command += " --testcase  FailoverScenario"
        return command


class Rebalance(object):
    def __init__(self, *args, **kwargs):
        self.count = kwargs['count']
        self.ept = kwargs['ept']
        self.rebalance_mode = kwargs['rebalance_mode']

    def build_args(self):
        command = " --rebalance-mode {0}".format(self.rebalance_mode)
        command += " --rebalance-ept {0}".format(self.ept)
        command += " --rebalance-count {0}".format(self.count)
        command += " --testcase  RebalanceScenario"
        return command


class ServiceFailure(object):
    def __init__(self, *args, **kwargs):
        self.service_restore_delay = SVCRESTORE_DELAY
        self.service_action = kwargs['service_action']
        self.count = kwargs['count']
        self.service_name = kwargs['service_name']

    def build_args(self):
        command = " --service-restore-delay {0}".format(self.service_restore_delay)
        command += " --service-action {0}".format(self.service_action)
        command += " --service-count  {0}".format(self.count)
        command += " --service-name {0}".format(self.service_name)
        command += " --testcase  ServiceFailureScenario"
        return command


class Bucket(object):
    def __init__(self, *args, **kwargs):
        self.bucket_type = kwargs['bucket_type']
        self.bucket_auth = kwargs['bucket_auth']

    def build_args(self):
        command = " --bucket-type {0}".format(self.bucket_type)
        if self.bucket_auth == "SASL":
            command += " --bucket-name protected"
            command += " --bucket-password secret"
        return command


class Cluster(object):
    def __init__(self, *args, **kwargs):
        self.nodes = kwargs['nodes']
        self.username = kwargs['username']
        self.password = kwargs['password']
        self.use_ssl = kwargs['ssl']
        self.altaddr = False
        if kwargs['altaddr']:
            self.altaddr = kwargs['altaddr']

    def build_args(self):
        command = ""
        for node in self.nodes:
            command += " --cluster_node=" + node
        command += " --cluster_ssh-username=" + self.username
        command += " --cluster_ssh-password=" + self.password
        command += " --cluster_ssl=" + self.use_ssl
        command += " --altaddr=" + self.altaddr
        return command


class Workload(object):
    def __init__(self, *args, **kwargs):
        self.name = kwargs['workload']
        self.instance = kwargs['instance_count']

    def build_args(self):
        command = " --workload=" + self.name
        command += " --kv-nthreads=" + str(self.instance)
        return command


class Driver(object):
    def __init__(self, *args, **kwargs):
        self.path = kwargs['nodes']
        self.driver_path = kwargs['driver_path']
        self.driver_args = kwargs['driver_args']
        self.port = 8050

    def build_args(self):
        command = " -C share/rexec"
        command += " --rexec_path=" + self.driver_path
        command += " --rexec_args=" + self.driver_args
        command += " --rexec_port=" + self.port
        command += " --rexec_args=-l " + self.port
        return command

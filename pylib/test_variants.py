from jscheduler.variants import KVVariant

variant = KVVariant(cluster_count=4, nodes=['1','2'], username='Administrator', password='password')
list = variant.get_failover_variants()
list2 = variant.get_rebalance_variants()
list3 = variant.get_svcfailure_variants()
print list.__len__() + list2.__len__() + list3.__len__()
print 0x40 | 0x2000
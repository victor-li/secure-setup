firewall {
    all-ping enable
    broadcast-ping disable
    config-trap disable
    ipv6-receive-redirects disable
    ipv6-src-route disable
    ip-src-route disable
    log-martians enable
    name LAN-IN {
        default-action drop
        rule 10 {
            action accept
            state {
                established enable
                related enable
            }
        }
    }
    name LAN-LOCAL {
        default-action drop
        rule 10 {
            action accept
            state {
                established enable
                related enable
            }
        }
    }
    receive-redirects disable
    send-redirects enable
    source-validation disable
    syn-cookies enable
    twa-hazards-protection disable
}
interfaces {
    ethernet eth0 {
        address dhcp
        description "Mac Private"
        duplex auto
        smp_affinity auto
        speed auto
    }
    ethernet eth1 {
        address dhcp
        description LAN
        duplex auto
        firewall {
            in {
                name LAN-IN
            }
            local {
                name LAN-LOCAL
            }
        }
        smp_affinity auto
        speed auto
    }
    ethernet eth2 {
        address 10.0.1.1/24
        description "PIA"
        duplex auto
        smp_affinity auto
        speed auto
    }
    loopback lo {
    }
    openvpn vtun1 {
        description "PIA VPN"
        encryption aes128
        mode client
        openvpn-option --comp-lzo
        openvpn-option "--verb 3"
        openvpn-option "--auth-user-pass /config/auth/pia/pia-secret.txt"
        openvpn-option "--script-security 2"
        openvpn-option "--resolv-retry infinite"
        openvpn-option --nobind
        openvpn-option --persist-key
        openvpn-option "--cipher aes-256-cbc"
        openvpn-option "--auth sha256"
        openvpn-option "--reneg-sec 0"
        persistent-tunnel
        protocol tcp-active
        remote-host swiss.privateinternetaccess.com
        remote-port 501
        tls {
            ca-cert-file /config/auth/pia/ca.rsa.4096.crt
            crl-file /config/auth/pia/crl.rsa.4096.pem
        }
    }
}
nat {
    source {
        rule 100 {
            outbound-interface vtun1
            source {
                address 10.0.1.0/24
            }
            translation {
                address masquerade
            }
        }
    }
}
service {
    dhcp-server {
        disabled false
        shared-network-name PIA-LAN {
            authoritative disable
            subnet 10.0.1.0/24 {
                default-router 10.0.1.1
                dns-server 10.0.1.1
                domain-name pia-network
                lease 86400
                start 10.0.1.9 {
                    stop 10.0.1.254
                }
            }
        }
    }
    dns {
        forwarding {
            cache-size 0
            listen-on eth2
            name-server 8.8.8.8
            name-server 8.8.4.4
        }
    }
    ssh {
        port 22
    }
}
system {
    config-management {
        commit-revisions 20
    }
    console {
        device ttyS0 {
            speed 9600
        }
    }
    host-name vyos
    login {
        user vyos {
            authentication {
                encrypted-password $6$Pe9B7JL1i7gCig$6tVFJqa7ya0hAtEqUICB9apbmfzEP4T.wJLlU2d5nOKo/gDMa5D4f7i67TE0VoB6Lqime9KiGecAH7QfbKdiy0
                plaintext-password ""
            }
            level admin
        }
    }
    ntp {
        server 0.pool.ntp.org {
        }
        server 1.pool.ntp.org {
        }
        server 2.pool.ntp.org {
        }
    }
    name-server 8.8.8.8
    name-server 8.8.4.4
    package {
        auto-sync 1
        repository community {
            components main
            distribution helium
            password ""
            url http://packages.vyos.net/vyos
            username ""
        }
    }
    syslog {
        global {
            facility all {
                level notice
            }
            facility protocols {
                level debug
            }
        }
    }
    time-zone Europe/Amsterdam
}


/* Warning: Do not remove the following line. */
/* === vyatta-config-version: "cluster@1:config-management@1:conntrack-sync@1:conntrack@1:cron@1:dhcp-relay@1:dhcp-server@4:firewall@5:ipsec@4:nat@4:qos@1:quagga@2:system@6:vrrp@1:wanloadbalance@3:webgui@1:webproxy@1:zone-policy@1" === */
/* Release version: VyOS 1.1.7 */

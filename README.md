# Secure setup on a MacOS host

This repository is a guide to create your own secure setup on a MacOS host.
In this setup, all traffic from the virtual machines will be routed through a VPN tunnel.
For the VPN tunnel, Private Internet Access will be used as example in this guide.

## My setup

* MacOS Sierra (10.12.3)
* VMWare Fusion (8.5.0)
* Private Internet Access (PIA) VPN
* VyOS Routing VM (1.17)
* Kali and Ubuntu VM's

## Network Diagram
![Network Diagram v1](images/v1\_network\_diagram.png)

## VyOS Diagram

![VyOS Diagram v1](images/v1\_vyos\_diagram.png)

## Setup

### Install VyOS
* Download the latest stable image of VyOS here: [https://vyos.io/#downloads]()
* Create a new VM and choose `Install from disc or image` as installation method.
* Select the downloaded image of VyOS.
* Customize the VM settings to 2 GB hard disk capacity and 512 MB memory.
* Set the network adapter in private mode (Private to my Mac).
* Boot the VM, and press **return** to boot the Live CD image.
* Login to the system using the default `username`:`password` => `vyos`:`vyos`
* Type `install image` to install the image on disk.

<pre>
vyos@vyos:~$ install image
Welcome to the VyOS install program.  This script
will walk you through the process of installing the
VyOS image to a local hard drive.
Would you like to continue? (Yes/No) [Yes]: <b>yes</b>
Probing drives: OK
Looking for pre-existing RAID groups...none found.
The VyOS image will require a minimum 1000MB root.
Would you like me to try to partition a drive automatically
or would you rather partition it manually with parted?  If
you have already setup your partitions, you may skip this step

Partition (Auto/Parted/Skip) [Auto]: <b>[return]</b>

I found the following drives on your system:
 sda	2147MB


Install the image on? [sda]: <b>[return]</b>

This will destroy all data on /dev/sda.
Continue? (Yes/No) [No]: <b>yes</b>

How big of a root partition should I create? (1000MB - 2147MB) [2147]MB: <b>[return]</b>

Creating filesystem on /dev/sda1: OK
Done!
Mounting /dev/sda1...
What would you like to name this image? [1.1.7]:
OK.  This image will be named: 1.1.7
Copying squashfs image...
Copying kernel and initrd images...
Done!
I found the following configuration files:
    /config/config.boot
    /opt/vyatta/etc/config.boot.default
Which one should I copy to sda? [/config/config.boot]: <b>[return]</b>

Copying /config/config.boot to sda.
Enter password for administrator account
Enter password for user 'vyos': <b>vyos</b>  # You will change this later
Retype password for user 'vyos': <b>vyos</b>  # You will change this later
I need to install the GRUB boot loader.
I found the following drives on your system:
 sda	2147MB


Which drive should GRUB modify the boot partition on? [sda]: <b>[return]</b>

Setting up grub: OK
Done!
</pre>

* Run `reboot` to boot VyOS from disk

<pre>
vyos@vyos:~$ reboot
Proceed with reboot? (Yes/No) [No] <b>yes</b>
</pre>

* You can now unmount the CD drive from the VM if you want to.

### Setup SSH
Using your own terminal is much more convenient to interact with VyOS, e.g. if you want to copy and paste commands into the console.
Therefore we will set up SSH first through the KVM console.

First, enter the configuration mode.
Only in this mode you are allowed to edit configuration rules in VyOS.
The prompt symbol will change from `$` to `#`.

```
vyos@vyos:~$ configure
[edit]
vyos@vyos#
```

You can exit the configuration mode using the command `exit`.

Enable the eth0 interface in dhcp mode:

```
set interfaces ethernet eth0 address dhcp
```

Enable the SSH service of VyOS:

```
set service ssh port 22
```

Commit the changes, save them and exit the configuration mode.

```
vyos@vyos# commit
[ interfaces ethernet eth0 address dhcp ]
Starting DHCP client on eth0 ...

[ service ssh ]
Restarting OpenBSD Secure Shell server: sshd.

[edit]
vyos@vyos# save
Saving configuration to '/config/config.boot'...
Done
vyos@vyos# exit
vyos@vyos$
```

Now you should be able to SSH into VyOS with your own terminal.
You can look up the IP address of the VyOS VM with the following command:

<pre>
vyos@vyos:~$ show interfaces
Codes: S - State, L - Link, u - Up, D - Down, A - Admin Down
Interface        IP Address                        S/L  Description
---------        ----------                        ---  -----------
eth0             <b>172.16.1.130</b>/24                   u/u
lo               127.0.0.1/8                       u/u
                 ::1/128
</pre>

<pre>
$ ssh vyos@172.16.1.130
The authenticity of host '172.16.1.130 (172.16.1.130)' can't be established.
RSA key fingerprint is SHA256:5mIixFcwGJ53pxjgws0M6wlAhH1IhZq93z6V/xIrTVQ.
Are you sure you want to continue connecting (yes/no)? <b>yes</b>
Warning: Permanently added '172.16.1.130' (RSA) to the list of known hosts.
Welcome to VyOS
vyos@172.16.1.130's password: <b>vyos</b>
Linux vyos 3.13.11-1-amd64-vyos
Welcome to VyOS.
This system is open-source software. The exact distribution terms for
each module comprising the full system are described in the individual
files in /usr/share/doc/*/copyright.
vyos@vyos:~$ 
</pre>

It is easier to add the VyOS host to your SSH config, such that you do not have to type the IP address every time.
From now on, `ssh vyos` will be used in all commands.

##### ~/.ssh/config
```
Host vyos
        HostName 172.16.1.130
        User vyos
```

### Create and configure network adapters in VM's
First, we need to create a virtual network for the VM's which traffic needs to go through the PIA VPN tunnel.
Create a new virtual network in VMWare Fusion (`Preferences > Network > +`).
In my case, the new virtual network is named `vmnet4`.
Due to limitations of VMWare, you cannot change the name of the virtual network.
Uncheck all boxes, because VM's will only communicate to the routing VM.
Create and configure network adapters for the VM's as following:

#### VyOS VM
For the VyOS VM, create and configure three network adapters:

* eth0: Private to my Mac
* eth1: Bridged mode (Autodetect)
* eth2: vmnet4

#### VM's which traffic needs to go through the VPN tunnel
For other VM's, configure one network adapter:

* eth0: vmnet4

### Setup configuration
* Download the `config.boot` template [here](config.boot).
* Download the PIA OpenVPN config files here: [https://www.privateinternetaccess.com/openvpn/openvpn-strong-tcp.zip]()
* Create a file named `pia-secret.txt` with your PIA username and password:

##### pia-secret.txt
```
p12345678
password123
```

Execute the following commands in your Mac terminal:

```
# Unzip PIA OpenVPN config file
user@mac:~$ unzip openvpn-strong-tcp.zip

# Create the folder '/config/auth/pia' on the routing VM
user@mac:~$ ssh vyos mkdir -p /config/auth/pia

# Copy the content of 'openvpn-strong-tcp' to the pia folder
user@mac:~$ scp openvpn-strong-tcp/* vyos:/config/auth/pia

# Copy the credentials to VyOS
user@mac:~$ scp pia-secret.txt vyos0:/config/auth/pia

# Copy the template configuration file to VyOS
user@mac:~$ scp config.boot vyos:/config/config.boot
```

### PIA - Username/Password Authentication
Private Internet Access uses username/password authentication for VPN connections.
Therefore there is no cert-file or key-file needed.
However, VyOS requires a cert-file and a key-file for every OpenVPN tunnel interface.
To bypass the cert-file and key-file requirement, apply the following fix.

Download the patch [here](patch-ovpn-user-pass.txt) and run the following command:

```
user@mac:~$ ssh vyos sudo patch /opt/vyatta/share/perl5/Vyatta/OpenVPN/Config.pm < patch-ovpn-user-pass.txt
```

### Up and Running
The last step is to reboot the routing VM to load the configuration of the new `config.boot` file.
Normally it is also possible to load the configuration from file without reboot (`configure` > `load` > `commit` > `save` > `exit`).
However, the interfaces in the new `config.boot` do not have hardware addresses, but they will be automatically set during startup.

```
user@mac:~$ ssh vyos sudo reboot
```

Your secure setup is now up and running! 🎉

### What to do now?
#### Change password

```
set system login user vyos authentication plaintext-password mypassword
```

This command will automatically convert your plaintext password and store it as a secure password hash.

#### Add ssh-key

```
user@mac:~$ scp ~/.ssh/id_rsa.pub vyos:.

# SSH into VyOS and enter configuration mode
vyos@vyos# loadkey vyos id_rsa.pub

Done
[edit]
vyos@vyos# save
Saving configuration to '/config/config.boot'...
Done
[edit]
vyos@vyos# exit
exit
```

#### Change timezone

```
set system time-zone America/Los_Angeles
```

Tip: Use `TAB` to show options and to autocomplete.

#### Change PIA server

```
set interfaces openvpn vtun1 remote-host nl.privateinternetaccess.com
```

Find all other PIA servers [here](https://www.privateinternetaccess.com/pages/network).

#### Create your own custom configuration
This is my current setup, but VyOS has many features like firewalls, static and dynamic routing, tunnel interfaces, proxies and more.
If you want to learn more about VyOS, go to [https://wiki.vyos.net/wiki/User_Guide]().

#### Star this repo
If you like this guide, give this repository a star! ⭐️
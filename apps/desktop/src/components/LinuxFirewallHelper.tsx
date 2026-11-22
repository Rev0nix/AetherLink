import React, { useState } from "react";
import { Terminal, Copy, Check, Shield } from "lucide-react";

export interface FirewallProfile {
  id: string;
  name: string;
  distro: string;
  command: string;
  desc: string;
}

export const LINUX_FIREWALL_PROFILES: FirewallProfile[] = [
  {
    id: "ufw",
    name: "UFW",
    distro: "Ubuntu / Debian / Mint / Pop!_OS",
    command: "sudo ufw allow 45876/tcp && sudo ufw allow 5353/udp",
    desc: "Uncomplicated Firewall for Ubuntu, Debian, Linux Mint, and derivatives",
  },
  {
    id: "firewalld",
    name: "Firewalld",
    distro: "Fedora / RHEL / CentOS / Rocky",
    command:
      "sudo firewall-cmd --add-port=45876/tcp --add-port=5353/udp --permanent && sudo firewall-cmd --reload",
    desc: "Dynamic firewall daemon for Fedora, Red Hat Enterprise Linux, CentOS, and AlmaLinux",
  },
  {
    id: "iptables",
    name: "iptables",
    distro: "Arch Linux / Manjaro / Generic",
    command:
      "sudo iptables -I INPUT -p tcp --dport 45876 -j ACCEPT && sudo iptables -I INPUT -p udp --dport 5353 -j ACCEPT",
    desc: "Direct Linux netfilter packet filter for Arch Linux, Manjaro, and custom kernels",
  },
  {
    id: "opensuse",
    name: "openSUSE",
    distro: "openSUSE Leap & Tumbleweed",
    command:
      "sudo firewall-cmd --zone=public --add-port=45876/tcp --add-port=5353/udp --permanent && sudo firewall-cmd --reload",
    desc: "Zone-based firewall configuration for openSUSE installations",
  },
  {
    id: "nftables",
    name: "nftables",
    distro: "Debian 12+ / Alpine / NixOS",
    command:
      "sudo nft add rule inet filter input tcp dport 45876 accept && sudo nft add rule inet filter input udp dport 5353 accept",
    desc: "Modern netfilter subsystem for next-generation Linux systems",
  },
];

interface LinuxFirewallHelperProps {
  compact?: boolean;
}

export const LinuxFirewallHelper: React.FC<LinuxFirewallHelperProps> = ({
  compact = false,
}) => {
  const [selectedId, setSelectedId] = useState<string>("ufw");
  const [copied, setCopied] = useState<boolean>(false);

  const activeProfile =
    LINUX_FIREWALL_PROFILES.find((p) => p.id === selectedId) ||
    LINUX_FIREWALL_PROFILES[0];

  const handleCopy = () => {
    navigator.clipboard.writeText(activeProfile.command);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="p-4 rounded-2xl bg-[var(--bg-card)] border border-amber-500/25 space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Shield size={16} className="text-[#FFB84D]" />
          <span className="text-xs font-bold text-[#FFB84D]">
            Linux Firewall Helper (All Distros)
          </span>
        </div>
        <button
          onClick={handleCopy}
          className="flex items-center gap-1.5 text-[11px] bg-[var(--bg-surface)] hover:bg-amber-500/20 text-[#FFB84D] px-2.5 py-1 rounded-lg font-mono transition cursor-pointer"
        >
          {copied ? (
            <Check size={13} className="text-[#35D49A]" />
          ) : (
            <Copy size={13} />
          )}
          <span>{copied ? "Copied Command!" : "Copy Command"}</span>
        </button>
      </div>

      <p className="text-[11px] text-[var(--text-secondary)] leading-relaxed">
        Linux firewalls block incoming TCP and UDP connections by default. Select
        your Linux distribution below to get the exact rule to allow port 45876:
      </p>

      {/* Distro Profile Selector */}
      <div className="flex flex-wrap gap-1.5">
        {LINUX_FIREWALL_PROFILES.map((profile) => {
          const isSelected = profile.id === selectedId;
          return (
            <button
              key={profile.id}
              onClick={() => setSelectedId(profile.id)}
              className={`px-2.5 py-1 rounded-lg text-[11px] font-medium transition cursor-pointer ${
                isSelected
                  ? "bg-[#FFB84D] text-black font-semibold shadow-sm"
                  : "bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:text-white border border-[var(--border-color)]"
              }`}
            >
              {profile.name}
            </button>
          );
        })}
      </div>

      {/* Active Distro Description */}
      <div className="text-[11px] text-[var(--text-muted)] flex items-center gap-1">
        <span className="font-semibold text-zinc-300">Target:</span>{" "}
        {activeProfile.distro} &bull; {activeProfile.desc}
      </div>

      {/* Terminal Command Output Box */}
      <div className="p-2.5 rounded-xl bg-black/50 font-mono text-[10px] text-zinc-200 border border-[var(--border-color)] select-all break-all flex items-center justify-between gap-2">
        <span>{activeProfile.command}</span>
      </div>
    </div>
  );
};

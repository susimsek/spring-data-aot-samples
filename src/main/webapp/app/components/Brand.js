import Image from 'next/image';

export default function Brand() {
  return (
    <span className="d-flex align-items-center gap-2">
      <Image src="/favicon.svg" alt="Notes logo" width={28} height={28} />
      <span>Notes</span>
    </span>
  );
}

export default function NotFound() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-800">404</h1>
        <p className="mt-2 text-gray-500">Página não encontrada</p>
        <a href="/" className="mt-4 inline-block text-blue-600 hover:underline">
          Voltar ao início
        </a>
      </div>
    </div>
  );
}

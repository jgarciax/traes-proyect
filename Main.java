import java.util.*;

public class Main {

    // nordo del arbol
    static class Nodo {
        String valor;
        Nodo izquierdo, derecho;

        Nodo(String valor) {
            this.valor = valor;
        }
    }

    // variables globales para el parser
    static String expresion;
    static int pos;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Evaluador de Expresiones Matemáticas   ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("Operadores permitidos: + - * / ^ √");
        System.out.println("Ejemplo: a + b - (c - b) + e\n");

        // 1. Leer expresión
        System.out.print("Ingresa la expresión: ");
        String entrada = sc.nextLine().trim();

        // 2. Validar caracteres
        if (!validarCaracteres(entrada)) {
            System.out.println("❌ Error: La expresión contiene caracteres no permitidos.");
            sc.close();
            return;
        }

        // Detectar variables
        Set<Character> vars = detectarVariables(entrada);

        // Pedir valores si hay variables
        Map<String, Double> valores = new HashMap<>();
        if (!vars.isEmpty()) {
            System.out.println("\nSe detectaron variables. Por favor ingresa sus valores:");
            int i = 1;
            for (char v : new TreeSet<>(vars)) {
                System.out.print("  " + i + ". " + v + " = ? ");
                while (!sc.hasNextDouble()) {
                    System.out.print("     Valor inválido, intenta de nuevo: ");
                    sc.next();
                }
                valores.put(String.valueOf(v), sc.nextDouble());
                i++;
            }
        }

        // Construir arbol
        try {
            expresion = entrada.replaceAll("\\s+", "");
            pos = 0;
            Nodo raiz = parsearSumaResta();

            // muestra el arbol
            System.out.println("\n📌 Árbol de expresión:");
            imprimirArbol(raiz, "", true);

            // evaluar resultado
            double resultado = evaluar(raiz, valores);
            System.out.println("\n✅ Resultado: " + resultado);

        } catch (Exception e) {
            System.out.println("❌ Error al procesar la expresión: " + e.getMessage());
        }

        sc.close();
    }

    // validacion de caracteres permitidos
    static boolean validarCaracteres(String expr) {
        // permitidos: letras, digitos, operadores, parentesis, punto, espacios
        return expr.matches("[a-zA-Z0-9+\\-*/^√().\\s]+");
    }

    // detectar letras
    static Set<Character> detectarVariables(String expr) {
        Set<Character> vars = new LinkedHashSet<>();
        for (char c : expr.toCharArray()) {
            if (Character.isLetter(c)) {
                vars.add(c);
            }
        }
        return vars;
    }

    // ─────────────────────────────────────────
    //  PARSER RECURSIVO DESCENDENTE
    // ─────────────────────────────────────────

    // Nivel 1: + y -
    static Nodo parsearSumaResta() {
        Nodo nodo = parsearMultDiv();
        while (pos < expresion.length() &&
               (expresion.charAt(pos) == '+' || expresion.charAt(pos) == '-')) {
            char op = expresion.charAt(pos++);
            Nodo derecho = parsearMultDiv();
            Nodo padre = new Nodo(String.valueOf(op));
            padre.izquierdo = nodo;
            padre.derecho = derecho;
            nodo = padre;
        }
        return nodo;
    }

    // Nivel 2: * y /
    static Nodo parsearMultDiv() {
        Nodo nodo = parsearPotencia();
        while (pos < expresion.length() &&
               (expresion.charAt(pos) == '*' || expresion.charAt(pos) == '/')) {
            char op = expresion.charAt(pos++);
            Nodo derecho = parsearPotencia();
            Nodo padre = new Nodo(String.valueOf(op));
            padre.izquierdo = nodo;
            padre.derecho = derecho;
            nodo = padre;
        }
        return nodo;
    }

    // Nivel 3: ^ (potencia)
    static Nodo parsearPotencia() {
        Nodo nodo = parsearUnario();
        if (pos < expresion.length() && expresion.charAt(pos) == '^') {
            pos++;
            Nodo derecho = parsearPotencia(); // Asociatividad derecha
            Nodo padre = new Nodo("^");
            padre.izquierdo = nodo;
            padre.derecho = derecho;
            return padre;
        }
        return nodo;
    }

    // Nivel 4: unario (raíz √, negativo -)
    static Nodo parsearUnario() {
        if (pos < expresion.length() && expresion.charAt(pos) == '√') {
            pos++;
            Nodo hijo = parsearUnario();
            Nodo nodo = new Nodo("√");
            nodo.derecho = hijo;
            return nodo;
        }
        if (pos < expresion.length() && expresion.charAt(pos) == '-') {
            pos++;
            Nodo hijo = parsearUnario();
            Nodo nodo = new Nodo("neg");
            nodo.derecho = hijo;
            return nodo;
        }
        return parsearPrimario();
    }

    // Nivel 5: paréntesis, números y variables
    static Nodo parsearPrimario() {
        if (pos >= expresion.length())
            throw new RuntimeException("Expresión incompleta en posición " + pos);

        char c = expresion.charAt(pos);

        // ())
        if (c == '(') {
            pos++;
            Nodo nodo = parsearSumaResta();
            if (pos >= expresion.length() || expresion.charAt(pos) != ')')
                throw new RuntimeException("Falta paréntesis de cierre ')'");
            pos++;
            return nodo;
        }

        // numero
        if (Character.isDigit(c) || c == '.') {
            StringBuilder sb = new StringBuilder();
            while (pos < expresion.length() &&
                   (Character.isDigit(expresion.charAt(pos)) || expresion.charAt(pos) == '.')) {
                sb.append(expresion.charAt(pos++));
            }
            return new Nodo(sb.toString());
        }

        // Variable
        if (Character.isLetter(c)) {
            pos++;
            return new Nodo(String.valueOf(c));
        }

        throw new RuntimeException("Carácter inesperado: '" + c + "' en posición " + pos);
    }

    // revision del arbol
    static double evaluar(Nodo nodo, Map<String, Double> valores) {
        if (nodo == null) throw new RuntimeException("Nodo nulo");

        String v = nodo.valor;

        // Hoja: nmero
        try { return Double.parseDouble(v); } catch (NumberFormatException ignored) {}

        // Hoja: variable
        if (v.length() == 1 && Character.isLetter(v.charAt(0))) {
            if (!valores.containsKey(v))
                throw new RuntimeException("Variable '" + v + "' sin valor asignado");
            return valores.get(v);
        }

        // Operadores binarios
        double izq = (nodo.izquierdo != null) ? evaluar(nodo.izquierdo, valores) : 0;
        double der = evaluar(nodo.derecho, valores);

        switch (v) {
            case "+":   return izq + der;
            case "-":   return izq - der;
            case "*":   return izq * der;
            case "/":
                if (der == 0) throw new RuntimeException("División por cero");
                return izq / der;
            case "^":   return Math.pow(izq, der);
            case "√":   return Math.sqrt(der);
            case "neg": return -der;
            default: throw new RuntimeException("Operador desconocido: " + v);
        }
    }

    static void imprimirArbol(Nodo nodo, String prefijo, boolean esUltimo) {
        if (nodo == null) return;
        System.out.println(prefijo + (esUltimo ? "└── " : "├── ") + nodo.valor);
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        if (nodo.izquierdo != null || nodo.derecho != null) {
            if (nodo.izquierdo != null)
                imprimirArbol(nodo.izquierdo, nuevoPrefijo, nodo.derecho == null);
            if (nodo.derecho != null)
                imprimirArbol(nodo.derecho, nuevoPrefijo, true);
        }
    }
}
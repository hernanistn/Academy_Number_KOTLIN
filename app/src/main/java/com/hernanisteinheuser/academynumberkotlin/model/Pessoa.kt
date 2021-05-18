package com.hernanisteinheuser.academynumberkotlin.model

class Pessoa {
    var nome : String = ""
    var vaga : String = ""
    var idade : Int? = null
    var estado: String = ""

    constructor(nome: String, vaga: String, idade: Int, estado: String){
        this.nome = nome
        this.vaga = vaga
        this.idade = idade
        this.estado = estado
    }

}
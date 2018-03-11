package model

case class User(id: Int ,
                name: String,
                username: String,
                email: String,
                address: Address,
                phone: String,
                website: String,
                company: Company)

case class Address(street: String,
                  suite: String,
                  city: String,
                  zipcode: String,
                  geo: Geo)

case class Geo(lat: String, lng: String)
case class Company(name: String, catchPhrase: String, bs: String)
